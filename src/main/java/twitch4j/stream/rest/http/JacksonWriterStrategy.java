package twitch4j.stream.rest.http;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.lang.reflect.Type;
import java.util.Objects;
import javax.annotation.Nullable;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientRequest;

/**
 * Write to a request from an {@code Object} to a JSON {@code String} using Jackson 2.9.
 */
public class JacksonWriterStrategy implements WriterStrategy<Object> {

	private final ObjectMapper objectMapper;

	public JacksonWriterStrategy(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean canWrite(@Nullable Class<?> type, @Nullable String contentType) {
		if (type == null || contentType == null || !contentType.startsWith("application/json")) {
			return false;
		}
		Class<?> rawClass = getJavaType(type).getRawClass();

		return (Object.class == rawClass)
				|| !String.class.isAssignableFrom(rawClass) && objectMapper.canSerialize(rawClass);
	}

	@Override
	public Mono<Void> write(HttpClientRequest request, @Nullable Object body) {
		Objects.requireNonNull(request);
		Objects.requireNonNull(body);
		try {
			return request.sendString(Mono.just(objectMapper.writeValueAsString(body))).then();
		} catch (Exception e) {
			throw Exceptions.propagate(e);
		}
	}

	private JavaType getJavaType(Type type) {
		TypeFactory typeFactory = this.objectMapper.getTypeFactory();
		return typeFactory.constructType(type);
	}
}
