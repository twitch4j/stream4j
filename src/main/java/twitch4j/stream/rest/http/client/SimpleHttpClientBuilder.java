package twitch4j.stream.rest.http.client;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.ipc.netty.http.client.HttpClient;
import twitch4j.stream.rest.http.ReaderStrategy;
import twitch4j.stream.rest.http.WriterStrategy;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
class SimpleHttpClientBuilder implements SimpleHttpClient.Builder {

	private final HttpHeaders headers = new DefaultHttpHeaders();
	private final List<ReaderStrategy<?>> readerStrategies = new ArrayList<>();
	private final List<WriterStrategy<?>> writerStrategies = new ArrayList<>();
	private String baseUrl = "";

	@Override
	public SimpleHttpClient.Builder baseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	@Override
	public SimpleHttpClient.Builder defaultHeader(String key, String value) {
		headers.add(key, value);
		return this;
	}

	@Override
	public SimpleHttpClient.Builder writerStrategy(WriterStrategy<?> strategy) {
		writerStrategies.add(strategy);
		return this;
	}

	@Override
	public SimpleHttpClient.Builder readerStrategy(ReaderStrategy<?> strategy) {
		readerStrategies.add(strategy);
		return this;
	}

	@Override
	public SimpleHttpClient build() {
		if (baseUrl == null || baseUrl.equals("")) {
			throw new NullPointerException("Base URL must be not empty.");
		} else if (!baseUrl.matches("(http[s]?)://(.+)")) {
			throw new IllegalArgumentException("Base URL must contain a URL");
		}
		return new SimpleHttpClient(HttpClient.create(options -> options.compression(true)), baseUrl, headers, writerStrategies, readerStrategies);
	}
}
