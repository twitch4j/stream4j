package twitch4j.stream.util;

import com.google.common.collect.Multimap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RouteUtils {

	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([\\w.]+)}");

	public static String expand(String template, Object... variables) {
		StringBuffer buf = new StringBuffer();
		Matcher matcher = PARAMETER_PATTERN.matcher(template);
		int index = 0;
		while (matcher.find()) {
			matcher.appendReplacement(buf, variables[index++].toString());
		}
		matcher.appendTail(buf);
		return buf.toString();
	}

	public static String expandQuery(String uri, @Nullable Multimap<String, ?> values) {
		if (values != null && !values.isEmpty()) {
			if (!uri.contains("?")) {
				uri += "?";
			}
			uri += values.entries().stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue())
					.collect(Collectors.joining("&"));
		}
		return uri;
	}

	@Nullable
	public static String getMajorParam(String template, String complete) {
		int start = template.indexOf('{');
		if (start == -1) {
			return null;
		}

		int end = complete.indexOf('/', start);
		if (end == -1) {
			end = complete.length();
		}

		return complete.substring(start, end);
	}
}
