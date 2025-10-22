package com.example.attendance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Json {
    private Json() {
    }

    public static Map<String, Object> parseObject(String json) {
        return new Parser(json).parseObject();
    }

    public static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return '"' + escapeString(s) + '"';
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append('"').append(escapeString(Objects.toString(entry.getKey()))).append('"')
                        .append(':')
                        .append(toJson(entry.getValue()));
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            boolean first = true;
            for (Object element : list) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(element));
            }
            builder.append(']');
            return builder.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    public static String escapeString(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static final class Parser {
        private final String source;
        private int position;

        Parser(String source) {
            this.source = source.trim();
        }

        Map<String, Object> parseObject() {
            skipWhitespace();
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                position++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                char next = expect(',', '}');
                if (next == '}') {
                    break;
                }
            }
            return result;
        }

        private Object parseValue() {
            skipWhitespace();
            char c = peek();
            if (c == '"') {
                return parseString();
            }
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == 't' && match("true")) {
                return Boolean.TRUE;
            }
            if (c == 'f' && match("false")) {
                return Boolean.FALSE;
            }
            if (c == 'n' && match("null")) {
                return null;
            }
            return parseNumber();
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                position++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                char next = expect(',', ']');
                if (next == ']') {
                    break;
                }
            }
            return list;
        }

        private Number parseNumber() {
            int start = position;
            if (peek() == '-') {
                position++;
            }
            while (position < source.length() && Character.isDigit(source.charAt(position))) {
                position++;
            }
            if (position < source.length() && source.charAt(position) == '.') {
                position++;
                while (position < source.length() && Character.isDigit(source.charAt(position))) {
                    position++;
                }
            }
            String number = source.substring(start, position);
            if (number.contains(".")) {
                return Double.parseDouble(number);
            }
            return Long.parseLong(number);
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (position < source.length()) {
                char c = source.charAt(position++);
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    char escaped = source.charAt(position++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            String hex = source.substring(position, position + 4);
                            builder.append((char) Integer.parseInt(hex, 16));
                            position += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid escape sequence: " + escaped);
                    }
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        private void skipWhitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }

        private char expect(char... choices) {
            if (position >= source.length()) {
                throw new IllegalArgumentException("Unexpected end of input");
            }
            char c = source.charAt(position++);
            for (char choice : choices) {
                if (c == choice) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Expected one of " + new String(choices) + " but found " + c);
        }

        private char peek() {
            if (position >= source.length()) {
                return '\0';
            }
            return source.charAt(position);
        }

        private boolean match(String expected) {
            if (!source.startsWith(expected, position)) {
                return false;
            }
            position += expected.length();
            return true;
        }
    }
}
