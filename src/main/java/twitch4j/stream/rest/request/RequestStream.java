package twitch4j.stream.rest.request;

import io.netty.handler.codec.http.HttpHeaders;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.netty.http.client.HttpClientResponse;
import reactor.retry.BackoffDelay;
import reactor.retry.Retry;
import reactor.retry.RetryContext;
import reactor.util.function.Tuple2;
import twitch4j.stream.rest.http.client.ClientException;
import twitch4j.stream.rest.http.client.ExchangeFilter;
import twitch4j.stream.rest.http.client.SimpleHttpClient;
import twitch4j.stream.util.RouteUtils;

/**
 * A stream of {@link twitch4j.stream.rest.request.TwitchRequest TwitchRequests}. Any number of items may be
 * {@link #push(Tuple2)} written to the stream. However, the
 * {@link twitch4j.stream.rest.request.RequestStream.Reader reader} ensures that only one is read at a time. This
 * linearization ensures proper ratelimit handling.
 *
 * @param <T> The type of items in the stream.
 */
@RequiredArgsConstructor
class RequestStream<T> {

	private final EmitterProcessor<Tuple2<MonoProcessor<T>, TwitchRequest<T>>> backing = EmitterProcessor.create(false);
	private final SimpleHttpClient httpClient;
	private final Duration defaultDuration;
	/**
	 * The retry function used for reading and completing HTTP requests. The back off is determined by the ratelimit
	 * headers returned by Twitch in the event of a 429. If the Helix endpoint is being globally ratelimited, the back off is
	 * applied to the concurrent rate limits if have a some condition:
	 * <ul>
	 *     <li>Default Rate Limit request for Helix is <b>30</b> per minute per IP</li>
	 *     <li>Bearer Rate Limit request for Helix is <b>120</b> per minute per Client ID</li>
	 *     <li>Default Rate Limit request for Kraken
	 *     (<a href="https://dev.twitch.tv/docs/v5/#which-api-version-can-you-use">deprecated</a>)
	 *     is <b>1</b> per second no matter what</li>
	 * </ul>
	 * Otherwise, it is applied only to this stream.
	 */
	private final Retry<AtomicLong> retryFactory = Retry.onlyIf((Predicate<RetryContext<AtomicLong>>) ctx -> {
		Throwable exception = ctx.exception();
		if (exception instanceof ClientException) {
			ClientException clientException = (ClientException) exception;
			if (clientException.getStatus().code() == 429) { // TODO: retry on other status codes?
				long resetAt = Long.parseLong(clientException.getHeaders().get("Ratelimit-Reset"));
				long unixTime = clientException.getHeaders().getTimeMillis("Date") / 1000;
				ctx.applicationContext().set(resetAt - unixTime);
				return true;
			}
		}
		return false;
	}).backoff(context -> {
		if (context.applicationContext() == null) {
			return new BackoffDelay(Duration.ZERO);
		}

		long delay = ((AtomicLong) context.applicationContext()).get();
		((AtomicLong) context.applicationContext()).set(0L);
		return new BackoffDelay(Duration.ofMillis(delay));
	}).withApplicationContext(new AtomicLong());

	void push(Tuple2<MonoProcessor<T>, TwitchRequest<T>> request) {
		backing.onNext(request);
	}

	void start() {
		read().subscribe(new Reader());
	}

	private Mono<Tuple2<MonoProcessor<T>, TwitchRequest<T>>> read() {
		return backing.next();
	}

	/**
	 * Reads and completes one request from the stream at a time. If a request fails, it is retried according to the
	 * {@link #retryFactory retry function}. The reader may wait in between each request if preemptive ratelimiting is
	 * necessary according to the response headers.
	 *
	 * @see #sleepTime
	 */
	private class Reader implements Consumer<Tuple2<MonoProcessor<T>, TwitchRequest<T>>> {

		private volatile Duration sleepTime = Duration.ZERO;
		private final Map<String, Duration> tokenSleepTime = new LinkedHashMap<>();

		@SuppressWarnings("ConstantConditions")
		@Override
		public void accept(Tuple2<MonoProcessor<T>, TwitchRequest<T>> tuple) {
			MonoProcessor<T> callback = tuple.getT1();
			TwitchRequest<T> req = tuple.getT2();
			String token = (req.headers() != null && req.headers().containsKey("Authorization")) ? req.headers().get("Authorization").iterator().next() : null;
			ExchangeFilter exchangeFilter = ExchangeFilter.builder()
					.requestFilter(request -> Optional.ofNullable(req.getHeaders())
							.ifPresent(headers -> headers.forEach(request::header)))
					.responseFilter(response -> {
						HttpHeaders headers = response.responseHeaders();
						int remaining = headers.getInt("Ratelimit-Remaining", -1);

						if (remaining == 0) {
							long resetAt = Long.parseLong(headers.get("Ratelimit-Reset"));
							long unixTime = headers.getTimeMillis("Date") / 1000;
							if (headers.getInt("Ratelimit-Limit") == 120 && token != null) {
								tokenSleepTime.put(token, Duration.ofSeconds(resetAt - unixTime));
							} else {
								sleepTime = Duration.ofSeconds(resetAt - unixTime);
							}
						}
					})
					.build();

			httpClient.exchange(req.getRoute().getMethod(),
							RouteUtils.expandQuery(req.getCompleteUri(), req.getQueryParams()), req.getBody(),
							req.getRoute().getResponseType(), exchangeFilter)
					.retryWhen(retryFactory)
					.materialize()
					.subscribe(signal -> {
						if (signal.isOnSubscribe()) {
							callback.onSubscribe(signal.getSubscription());
						} else if (signal.isOnNext()) {
							callback.onNext(signal.get());
						} else if (signal.isOnError()) {
							callback.onError(signal.getThrowable());
						} else if (signal.isOnComplete()) {
							callback.onComplete();
						}

						if (httpClient.getBaseUrl().contains("helix")) {
							if (token != null) {
								Mono.delay(tokenSleepTime.get(token))
										.subscribe(l -> {
											tokenSleepTime.remove(token);
											read().subscribe(this);
										});
							} else {
								Mono.delay(sleepTime).subscribe(l -> {
									sleepTime = Duration.ZERO;
									read().subscribe(this);
								});
							}
						} else if (httpClient.getBaseUrl().contains("kraken")) {
							Mono.delay(Duration.ofSeconds(1))
									.subscribe(l -> read().subscribe(this));
						} else {
							Mono.delay(defaultDuration)
									.subscribe(l -> read().subscribe(this));
						}
					});
		}
	}
}
