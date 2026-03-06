package io.github.voronkov.easyapialert;

import java.util.regex.Pattern;

public final class RouteNormalizer {

    private RouteNormalizer() {}

    private static final Pattern NUMERIC = Pattern.compile("^\\d+$");

    private static final Pattern UUID = Pattern.compile(
            "^(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );

    private static final Pattern MONGO_OBJECT_ID = Pattern.compile("(?i)^[0-9a-f]{24}$");

    private static final Pattern ULID = Pattern.compile("(?i)^[0-9a-hjkmnp-tv-z]{26}$");

    private static final Pattern VERSION = Pattern.compile("(?i)^v\\d+$");

    public static String normalize(String path) {
        if (path == null || path.isBlank()) return path;

        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);

        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            String seg = parts[i];
            if (seg.isEmpty()) {
                continue;
            }

            if (VERSION.matcher(seg).matches()) {
                continue;
            }

            if (isIdLike(seg)) {
                parts[i] = "{id}";
            }
        }

        StringBuilder sb = new StringBuilder(path.length());
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            sb.append('/').append(p);
        }
        return sb.isEmpty() ? "/" : sb.toString();
    }

    private static boolean isIdLike(String seg) {
        return NUMERIC.matcher(seg).matches()
                || UUID.matcher(seg).matches()
                || MONGO_OBJECT_ID.matcher(seg).matches()
                || ULID.matcher(seg).matches();
    }
}