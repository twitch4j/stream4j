package twitch4j.stream.rest.http.client;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;
import lombok.ToString;
import reactor.core.publisher.Mono;
import twitch4j.stream.json.Error;

@Getter
@ToString
public class ClientException extends RuntimeException {

	private final HttpResponseStatus status;
	private final HttpHeaders headers;
	private final Mono<Error> errorResponse;

	public ClientException(HttpResponseStatus status, HttpHeaders headers, Mono<Error> errorResponse) {
		this.status = status;
		this.headers = headers;
		this.errorResponse = errorResponse;
	}
}
