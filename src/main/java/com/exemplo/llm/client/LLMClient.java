package com.exemplo.llm.client;

import com.exemplo.llm.model.ChatRequest;
import com.exemplo.llm.model.LLMProvider;

/**
 * Contrato único para conversar com qualquer LLM (Strategy Pattern). O código de
 * negócio depende apenas desta interface; trocar de provedor não muda o chamador.
 */
public interface LLMClient {

    /**
     * Envia a conversa e devolve o texto gerado pelo modelo.
     *
     * @throws LLMException em falhas HTTP, de conexão ou de formato da resposta
     */
    String chat(ChatRequest request);

    /** Provedor atendido por esta implementação. */
    LLMProvider provider();

    /** Modelo usado quando a requisição não especifica um. */
    String defaultModel();
}
