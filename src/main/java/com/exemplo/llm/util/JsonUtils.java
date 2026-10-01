package com.exemplo.llm.util;

import java.util.Optional;

/**
 * Utilitários mínimos de JSON sem bibliotecas externas: escape de strings e
 * extração de valores string por chave. Não é um parser completo — é suficiente
 * para os formatos de resposta conhecidos dos provedores.
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    /** Escapa um texto para uso dentro de uma string JSON (sem as aspas externas). */
    public static String escape(String text) {
        var sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            appendEscaped(sb, text.charAt(i));
        }
        return sb.toString();
    }

    /** Escapa e envolve em aspas: {@code ola "mundo"} vira {@code "ola \"mundo\""}. */
    public static String quote(String text) {
        return "\"" + escape(text) + "\"";
    }

    /**
     * Procura a primeira chave {@code key} com valor string, a partir da primeira
     * ocorrência de {@code anchorKey} (ex.: {@code "content"} depois de {@code "choices"}).
     */
    public static Optional<String> findString(String json, String key, String anchorKey) {
        int anchor = json.indexOf("\"" + anchorKey + "\"");
        return anchor < 0 ? Optional.empty() : findString(json, key, anchor);
    }

    /** Procura a primeira chave {@code key} com valor string no documento. */
    public static Optional<String> findString(String json, String key) {
        return findString(json, key, 0);
    }

    /**
     * Extrai uma mensagem de erro legível de um corpo de erro: {@code error.message}
     * (OpenAI/Gemini), {@code error} (Ollama) ou o próprio corpo abreviado.
     */
    public static String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "(corpo da resposta vazio)";
        }
        return findString(body, "message")
                .or(() -> findString(body, "error"))
                .orElseGet(() -> abbreviate(body, 300));
    }

    /** Corta o texto em {@code max} caracteres, sem quebras de linha. */
    public static String abbreviate(String text, int max) {
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= max ? singleLine : singleLine.substring(0, max) + "...";
    }

    private static void appendEscaped(StringBuilder sb, char c) {
        switch (c) {
            case '"' -> sb.append("\\\"");
            case '\\' -> sb.append("\\\\");
            case '\n' -> sb.append("\\n");
            case '\r' -> sb.append("\\r");
            case '\t' -> sb.append("\\t");
            case '\b' -> sb.append("\\b");
            case '\f' -> sb.append("\\f");
            default -> {
                if (c < 0x20) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    sb.append(c);
                }
            }
        }
    }

    private static Optional<String> findString(String json, String key, int fromIndex) {
        String needle = "\"" + key + "\"";
        for (int idx = json.indexOf(needle, fromIndex); idx >= 0; idx = json.indexOf(needle, idx + 1)) {
            int colon = skipWhitespace(json, idx + needle.length());
            if (colon < json.length() && json.charAt(colon) == ':') {
                int valueStart = skipWhitespace(json, colon + 1);
                boolean isString = valueStart < json.length() && json.charAt(valueStart) == '"';
                return isString ? Optional.of(readString(json, valueStart + 1)) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static int skipWhitespace(String json, int index) {
        int i = index;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        return i;
    }

    /** Lê uma string JSON a partir do caractere seguinte à aspa de abertura. */
    private static String readString(String json, int start) {
        var sb = new StringBuilder();
        int i = start;
        while (i < json.length() && json.charAt(i) != '"') {
            char c = json.charAt(i);
            if (c == '\\') {
                i = appendUnescaped(sb, json, i + 1);
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /** Decodifica a sequência de escape em {@code index} e retorna o próximo índice a ler. */
    private static int appendUnescaped(StringBuilder sb, String json, int index) {
        char esc = json.charAt(index);
        switch (esc) {
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'u' -> {
                sb.append((char) Integer.parseInt(json.substring(index + 1, index + 5), 16));
                return index + 5;
            }
            default -> sb.append(esc); // \" \\ \/
        }
        return index + 1;
    }
}
