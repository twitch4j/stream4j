package twitch4j.stream.rest.request;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import reactor.core.publisher.Mono;
import twitch4j.stream.rest.route.Route;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Encodes all of the needed information to make an HTTP request to Twitch.
 *
 * @param <T> The response type.
 * @since 1.0
 */
@Getter
@Setter
@Accessors(fluent = true, chain = true)
public class TwitchRequest<T> {

	private final Route<T> route;
	private final String completeUri;

	@Nullable
	private Object body;

	@Nullable
	private Multimap<String, Object> queryParams;

	@Nullable
	private Multimap<String, String> headers;

	public TwitchRequest(Route<T> route, String completeUri) {
		this.route = route;
		this.completeUri = completeUri;
	}

	Route<T> getRoute() {
		return route;
	}

	String getCompleteUri() {
		return completeUri;
	}

	@Nullable
	public Object getBody() {
		return body;
	}

	@Nullable
	public Multimap<String, ?> getQueryParams() {
		return queryParams;
	}

	@Nullable
	public Multimap<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Set the given synchronous {@link Object} as the body for the request.
	 *
	 * @param body the object to set as request body
	 * @return this request
	 */
	public TwitchRequest<T> body(Object body) {
		this.body = body;
		return this;
	}

	/**
	 * Add the given name and value as a request query parameter.
	 *
	 * @param key   the query parameter name
	 * @param value the query parameter value
	 * @return this request
	 */
	public TwitchRequest<T> query(String key, Object value) {
		if (queryParams == null) {
			queryParams = MultimapBuilder.SetMultimapBuilder.linkedHashKeys().hashSetValues().build();
		}
		queryParams.put(key, value);
		return this;
	}

	/**
	 * Adds the given names and values as request query parameters.
	 *
	 * @param params a map of query parameter names to values
	 * @return this request
	 */
	public TwitchRequest<T> query(Map<String, ?> params) {
		params.forEach(this::query);
		return this;
	}

	/**
	 * Adds the given key and value to the headers of this request.
	 *
	 * @param key   the header key
	 * @param value the header value
	 * @return this request
	 */
	public TwitchRequest<T> header(String key, String value) {
		if (headers == null) {
			headers = ImmutableSetMultimap.of();
		}
		headers.put(key.toLowerCase(), value);
		return this;
	}

	/**
	 * Exchange this request through the given {@link Router}.
	 *
	 * @param router a router that performs this request
	 * @return the result of this request
	 */
	public Mono<T> exchange(Router router) {
		return router.exchange(this);
	}
}
