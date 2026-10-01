package com.exemplo.llm.prompt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Template de prompt imutável com variáveis no formato {@code {{variavel}}}.
 *
 * <p>A substituição é feita em uma única passada: valores inseridos não são
 * reinterpretados como template, evitando "template injection" a partir de
 * dados do usuário.
 *
 * <pre>{@code
 * PromptTemplate.of("Olá, {{nome}}!").render(Map.of("nome", "Ana")); // "Olá, Ana!"
 * }</pre>
 *
 * @param template texto com placeholders
 */
public record PromptTemplate(String template) {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    public PromptTemplate {
        Objects.requireNonNull(template, "template");
        if (template.isBlank()) {
            throw new IllegalArgumentException("O template não pode ser vazio");
        }
    }

    public static PromptTemplate of(String template) {
        return new PromptTemplate(template);
    }

    /** Nomes das variáveis presentes, na ordem em que aparecem. */
    public Set<String> variables() {
        return VARIABLE.matcher(template).results()
                .map(m -> m.group(1))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Substitui todas as variáveis.
     *
     * @throws IllegalArgumentException se alguma variável não tiver valor
     */
    public String render(Map<String, ?> values) {
        Objects.requireNonNull(values, "values");
        List<String> missing = variables().stream().filter(v -> !values.containsKey(v)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Variáveis sem valor no template: " + missing);
        }
        return substitute(values);
    }

    /**
     * Substitui apenas as variáveis informadas e devolve um novo template com as
     * restantes ainda abertas (útil para compor prompts em etapas).
     */
    public PromptTemplate partial(Map<String, ?> values) {
        Objects.requireNonNull(values, "values");
        return of(substitute(values));
    }

    /** Compõe dois templates separados por uma linha em branco. */
    public PromptTemplate append(PromptTemplate other) {
        return of(template + "\n\n" + Objects.requireNonNull(other, "other").template());
    }

    private String substitute(Map<String, ?> values) {
        return VARIABLE.matcher(template).replaceAll(match -> replacementFor(match, values));
    }

    private static String replacementFor(MatchResult match, Map<String, ?> values) {
        String name = match.group(1);
        String value = values.containsKey(name) ? String.valueOf(values.get(name)) : match.group();
        return Matcher.quoteReplacement(value);
    }
}
