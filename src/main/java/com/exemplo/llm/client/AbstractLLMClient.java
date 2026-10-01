package com.exemplo.llm.client;

import com.exemplo.llm.model.ChatRequest;
import com.exemplo.llm.util.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Base dos clientes HTTP (Template Method): o fluxo enviar → validar status →
 * extrair conteúdo é comum; cada provedor implementa apenas o formato do corpo
 * da requisição e a leitura da resposta.
 */
public abstract class AbstractLLMClient implements LLMClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);
    private static final int MAX_BODY_IN_ERRORS = 300;

    private final HttpClient httpClient;
    private final String defaultModel;

    protected AbstractLLMClient(HttpClient httpClient, String defaultModel) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.defaultModel = Objects.requireNonNull(defaultModel, "defaultModel");
    }

    @Override
    public final String chat(ChatRequest request) {
        Objects.requireNonNull(request, "request");
        String model = request.modelOr(defaultModel);
        HttpResponse<String> response = send(buildHttpRequest(request, model));
        ensureSuccess(response);
        return extractContent(response.body());
    }

    @Override
    public String defaultModel() {
        return defaultModel;
    }

    /** Monta a requisição HTTP no formato do provedor. */
    protected abstract HttpRequest buildHttpRequest(ChatRequest request, String model);

    /** Extrai o texto gerado de uma resposta 2xx. */
    protected abstract String extractContent(String responseBody);

    /** Tempo máximo de espera pela resposta; modelos locais podem precisar de mais. */
    protected Duration requestTimeout() {
        return DEFAULT_TIMEOUT;
    }

    /** Dica exibida em falhas de conexão. */
    protected String connectionHint() {
        return "Verifique sua conexão de rede.";
    }

    /** Builder de POST JSON já com timeout e cabeçalhos padrão. */
    protected final HttpRequest.Builder jsonPost(URI uri, String jsonBody) {
        return HttpRequest.newBuilder(uri)
                .timeout(requestTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
    }

    /** Garante que o conteúdo extraído existe; caso contrário, falha com o corpo recebido. */
    protected final String requireContent(Optional<String> content, String responseBody) {
        return content.filter(text -> !text.isBlank())
                .orElseThrow(() -> new LLMException.InvalidResponse(provider(),
                        "Conteúdo não encontrado na resposta: "
                                + JsonUtils.abbreviate(responseBody, MAX_BODY_IN_ERRORS)));
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new LLMException.ConnectionFailed(provider(),
                    "Tempo limite de " + requestTimeout().toSeconds() + "s excedido.", e);
        } catch (IOException e) {
            String reason = Objects.toString(e.getMessage(), e.getClass().getSimpleName());
            throw new LLMException.ConnectionFailed(provider(),
                    "Falha de comunicação (" + reason + "). " + connectionHint(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException.ConnectionFailed(provider(), "Requisição interrompida.", e);
        }
    }

    private void ensureSuccess(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw LLMException.fromHttpStatus(provider(), status,
                    JsonUtils.extractErrorMessage(response.body()));
        }
    }
}
