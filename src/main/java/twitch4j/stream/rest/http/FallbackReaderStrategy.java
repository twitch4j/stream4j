package twitch4j.stream.rest.http;

import java.util.Objects;
import javax.annotation.Nullable;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientResponse;

/**
 * Read a response as a {@code String}, regardless of its type and response Content-Type. It serves as a "catch-all"
 * reader.
 */
public class FallbackReaderStrategy implements ReaderStrategy<String> {

	@Override
	public boolean canRead(@Nullable Class<?> type, @Nullable String contentType) {
		return true;
	}

	@Override
	public Mono<String> read(HttpClientResponse response, Class<String> responseType) {
		Objects.requireNonNull(response);
		return response.receive().aggregate().asString();
	}
}
