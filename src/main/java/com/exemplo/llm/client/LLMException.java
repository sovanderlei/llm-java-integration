package com.exemplo.llm.client;

import com.exemplo.llm.model.LLMProvider;

/**
 * Hierarquia selada de falhas de integração com LLMs. Por ser {@code sealed},
 * um {@code switch} com pattern matching sobre ela é verificado como exaustivo
 * pelo compilador.
 */
public abstract sealed class LLMException extends RuntimeException
        permits LLMException.Unauthorized, LLMException.RateLimited, LLMException.ServerError,
                LLMException.ClientError, LLMException.ConnectionFailed, LLMException.InvalidResponse {

    /** Usado quando não há resposta HTTP (ex.: falha de conexão). */
    public static final int NO_STATUS = 0;

    private final LLMProvider provider;
    private final int statusCode;

    private LLMException(LLMProvider provider, int statusCode, String message, Throwable cause) {
        super(format(provider, statusCode, message), cause);
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public LLMProvider provider() {
        return provider;
    }

    public int statusCode() {
        return statusCode;
    }

    /** Converte um status HTTP de erro na exceção correspondente, com mensagem clara. */
    public static LLMException fromHttpStatus(LLMProvider provider, int status, String detail) {
        return switch (status) {
            case 401, 403 -> new Unauthorized(provider, status,
                    "API key inválida ou sem permissão. Detalhe: " + detail);
            case 429 -> new RateLimited(provider,
                    "Limite de requisições/cota excedido. Detalhe: " + detail);
            case 404 -> new ClientError(provider, status,
                    "Modelo ou endpoint não encontrado. Detalhe: " + detail);
            default -> status >= 500
                    ? new ServerError(provider, status, "Erro interno do provedor. Detalhe: " + detail)
                    : new ClientError(provider, status, "Requisição rejeitada. Detalhe: " + detail);
        };
    }

    private static String format(LLMProvider provider, int statusCode, String message) {
        String status = statusCode == NO_STATUS ? "" : " HTTP " + statusCode;
        return "[" + provider + status + "] " + message;
    }

    /** HTTP 401/403: credencial ausente, inválida ou sem permissão. */
    public static final class Unauthorized extends LLMException {
        Unauthorized(LLMProvider provider, int status, String message) {
            super(provider, status, message, null);
        }
    }

    /** HTTP 429: rate limit ou cota esgotada. */
    public static final class RateLimited extends LLMException {
        RateLimited(LLMProvider provider, String message) {
            super(provider, 429, message, null);
        }
    }

    /** HTTP 5xx: falha no lado do provedor. */
    public static final class ServerError extends LLMException {
        ServerError(LLMProvider provider, int status, String message) {
            super(provider, status, message, null);
        }
    }

    /** Demais HTTP 4xx: parâmetros, modelo ou corpo inválidos. */
    public static final class ClientError extends LLMException {
        ClientError(LLMProvider provider, int status, String message) {
            super(provider, status, message, null);
        }
    }

    /** Sem resposta HTTP: rede, DNS, timeout ou serviço fora do ar. */
    public static final class ConnectionFailed extends LLMException {
        ConnectionFailed(LLMProvider provider, String message, Throwable cause) {
            super(provider, NO_STATUS, message, cause);
        }
    }

    /** HTTP 2xx, mas sem o conteúdo esperado (ex.: resposta bloqueada por filtro). */
    public static final class InvalidResponse extends LLMException {
        InvalidResponse(LLMProvider provider, String message) {
            super(provider, NO_STATUS, message, null);
        }
    }
}
