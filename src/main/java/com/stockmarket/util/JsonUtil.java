package com.stockmarket.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Lightweight JSON utility for serializing Java objects, Maps, and Lists to JSON,
 * and extracting simple JSON properties without requiring external third-party JARs.
 */
public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof java.util.Date || obj instanceof java.sql.Timestamp || obj instanceof java.sql.Date) {
            return "\"" + obj.toString() + "\"";
        }
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(arr[i]));
            }
            sb.append("]");
            return sb.toString();
        }

        // POJO reflection serialization
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            // Never serialize sensitive fields like password or passwordHash
            if ("password".equalsIgnoreCase(f.getName()) || "passwordHash".equalsIgnoreCase(f.getName())) continue;
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (!first) sb.append(",");
                sb.append("\"").append(f.getName()).append("\":");
                sb.append(toJson(val));
                first = false;
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static String successResponse(String message, Object data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", true);
        map.put("message", message);
        if (data != null) {
            map.put("data", data);
        }
        return toJson(map);
    }

    public static String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", false);
        map.put("message", message);
        return toJson(map);
    }

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Reads entire HTTP request body as a string.
     */
    public static String readRequestBody(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString().trim();
    }

    /**
     * Extracts a simple string property from a basic JSON string: {"prop":"val"}
     */
    public static String extractString(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        // Try unquoted number/bool
        String numPattern = "\"" + key + "\"\\s*:\\s*([0-9.]+|true|false)";
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(numPattern);
        java.util.regex.Matcher m2 = p2.matcher(json);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }
}
