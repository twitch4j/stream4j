package twitch4j.stream.rest.request;

import twitch4j.stream.util.RouteUtils;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Used to access the appropriate {@link twitch4j.stream.rest.request.RequestStream RequestStream} according to the bucket
 * that requests for the stream fall into.
 * <p>
 * Following the <a href="https://dev.twitch.tv/docs/api/guide/#rate-limits">
 * Twitch documentation</a>, requests belong to the same bucket if:
 * <ul>
 * <li>The {@link twitch4j.stream.rest.route.Route#uriTemplate uriTemplates} are equal.</li>
 * <li>The {@link #majorParam major parameters} are equal.</li>
 * </ul>
 * Note that HTTP method is <b>not</b> considered (requests fall into the same bucket even if the methods are different)
 * in all but one case. Requests on the message delete route fall into a separate bucket.
 * <p>
 * This is a value-based class.
 *
 * @since 1.0
 */
public final class BucketKey {

	private final String uriTemplate;
	@Nullable
	private final String majorParam;

	private BucketKey(String uriTemplate, String completeUri) {
		this.uriTemplate = uriTemplate;
		this.majorParam = RouteUtils.getMajorParam(uriTemplate, completeUri);
	}

	static BucketKey of(String uriTemplate, String completeUri) {
		return new BucketKey(uriTemplate, completeUri);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uriTemplate, majorParam);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		BucketKey bucket = (BucketKey) obj;

		return uriTemplate.equals(bucket.uriTemplate) && Objects.equals(majorParam, bucket.majorParam);
	}
}
