package com.exemplo.llm.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Mensagem imutável de uma conversa com o LLM.
 *
 * @param role    papel de quem fala
 * @param content texto da mensagem (não pode ser vazio)
 */
public record ChatMessage(Role role, String content) {

    /** Papéis neutros; cada cliente traduz para o vocabulário do seu provedor. */
    public enum Role {
        SYSTEM, USER, ASSISTANT;

        /** Nome em minúsculas, usado por OpenAI e Ollama. */
        public String wireName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public ChatMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("O conteúdo da mensagem não pode ser vazio");
        }
    }

    /** Instruções de comportamento (persona, regras, formato). */
    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    /** Entrada do usuário. */
    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    /** Resposta anterior do modelo (útil para histórico ou few-shot em formato de diálogo). */
    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }
}
