# 🤖 Prompt: Integração de LLMs com Java 21

> **Objetivo:** Gerar um projeto Java profissional que integre OpenAI, Gemini e Ollama com Prompt Engineering dinâmico.  
> **Data de criação:** 01/10/2026  
> **Nível:** Intermediário → Avançado  
> **Stack:** Java 21+ (JDK puro, sem frameworks)

---

## 🎯 PAPEL E CONTEXTO

Você é um **Arquiteto de Software Sênior especializado em Java 21+ e IA Generativa**, com mais de 10 anos de experiência em integração de APIs externas, design de APIs limpas e Prompt Engineering aplicado a sistemas corporativos.

Seu objetivo é entregar um **projeto Java profissional, pronto para produção**, que demonstre integração com múltiplos provedores de LLM e técnicas avançadas de Prompt Engineering embutidas no código.

---

## 📦 OBJETIVO DO PROJETO

Criar um projeto Java modular chamado `llm-java-integration` que:

1. **Integre 3 provedores de LLM** com uma interface unificada:
   - OpenAI (GPT-4o-mini, GPT-4o)
   - Google Gemini (gemini-2.0-flash, gemini-1.5-pro)
   - Ollama (modelos locais: llama3.2, qwen2.5-coder, codellama)

2. **Implemente um motor de Prompt Engineering dinâmico** com:
   - Templates com substituição de variáveis `{{variavel}}`
   - Biblioteca de prompts reutilizáveis com técnicas comprovadas
   - Suporte a formatação e composição

3. **Use apenas a biblioteca padrão do Java 21** (`java.net.http.HttpClient`, `records`, text blocks, pattern matching, sealed classes quando fizer sentido).
   **NÃO use Spring, Jackson, Gson ou qualquer framework externo** — o projeto deve ser autocontido e fácil de executar.

---

## 🏗️ ARQUITETURA ESPERADA

```
src/main/java/com/exemplo/llm/
├── model/
│   ├── LLMProvider.java          # Enum com URLs base
│   ├── ChatMessage.java          # Record (role, content) com factory methods
│   └── ChatRequest.java          # Builder pattern com validações
├── prompt/
│   ├── PromptTemplate.java       # Motor de templates com regex
│   └── PromptLibrary.java        # Prompts prontos (code review, debug, testes)
├── client/
│   ├── LLMClient.java            # Interface Strategy
│   ├── OpenAIClient.java         # Implementação OpenAI
│   ├── GeminiClient.java         # Implementação Gemini
│   └── OllamaClient.java         # Implementação Ollama (sem API key)
├── config/
│   └── LLMConfig.java            # Factory method + variáveis de ambiente
└── Main.java                     # Exemplos de uso
```

---

## ✅ REQUISITOS FUNCIONAIS

### Integração com LLMs

- [ ] Interface `LLMClient` com método `String chat(ChatRequest request)`
- [ ] Cada provedor deve tratar suas peculiaridades:
  - **OpenAI**: roles `system|user|assistant`, campo `messages[]`
  - **Gemini**: roles `user|model`, campo `contents[].parts[].text`, API key na URL
  - **Ollama**: endpoint local, sem autenticação, campo `options{}`
- [ ] Tratamento de erros HTTP (401, 429, 500) com mensagens claras
- [ ] Escape correto de JSON (aspas, quebras de linha, caracteres especiais)
- [ ] Extração do conteúdo da resposta (parse simplificado do JSON)

### Prompt Engineering

- [ ] `PromptTemplate` com substituição via regex `\{\{(\w+)\}\}`
- [ ] `PromptLibrary` com pelo menos 4 prompts prontos aplicando estas técnicas:
  - **Role Assignment**: definir persona do LLM
  - **Chain-of-Thought (CoT)**: "pense passo a passo"
  - **Few-Shot / Structured Output**: forçar formato de resposta
  - **Guardrails**: regras de segurança e escopo
- [ ] Controle de `temperature` e `maxTokens` via Builder

### Configuração

- [ ] API keys via variáveis de ambiente (`OPENAI_API_KEY`, `GEMINI_API_KEY`)
- [ ] Ollama não deve exigir chave
- [ ] Factory method `LLMConfig.createClient(LLMProvider)` com validação

---

## 🎨 REQUISITOS DE QUALIDADE DE CÓDIGO

- Use **Java 21** com todos os recursos modernos:
  - `record` para DTOs imutáveis
  - Text blocks (`"""..."""`) para JSON e prompts
  - Pattern matching com `switch` expressions
  - `var` quando apropriado
- Siga **SOLID**: cada classe com responsabilidade única
- Use **Builder Pattern** em `ChatRequest`
- Use **Strategy Pattern** nos clientes LLM
- **Métodos pequenos** (< 20 linhas), bem nomeados
- **Comentários Javadoc** em classes públicas e métodos importantes
- **Tratamento de exceções** explícito (não engolir erros)
- **Imutabilidade** onde possível (`List.copyOf`, records)

---

## 📝 EXEMPLOS DE USO OBRIGATÓRIOS NO `Main.java`

O `Main.java` deve demonstrar 3 casos reais:

1. **Code Review**: enviar um trecho de código Java mal escrito e pedir revisão com técnica de Role Assignment + Structured Output
2. **Análise de Bug**: enviar um stack trace e pedir diagnóstico com Chain-of-Thought
3. **Geração de Testes**: pedir testes JUnit 5 para um método com Structured Output e `temperature=0.0`

Cada exemplo deve:

- Usar `PromptLibrary` (não escrever prompts inline)
- Configurar `temperature` e `maxTokens` adequadamente
- Imprimir a resposta formatada no console

---

## 📚 DOCUMENTAÇÃO

Gere também um `README.md` com:

1. **Visão geral** do projeto (1 parágrafo)
2. **Pré-requisitos** (Java 21, Ollama opcional)
3. **Como executar** cada provedor (comandos bash)
4. **Estrutura do projeto** (árvore de arquivos)
5. **Técnicas de Prompt Engineering** usadas (tabela)
6. **Como estender** (adicionar novo provedor, novo prompt)
7. **Próximos passos** (mencionar Spring AI, streaming, RAG)

---

## 🚫 RESTRIÇÕES (NÃO FAÇA)

- ❌ NÃO use dependências externas (Maven/Gradle) — apenas JDK puro
- ❌ NÃO use bibliotecas de JSON — faça parse manual simples
- ❌ NÃO implemente streaming (SSE) — foque em requisições síncronas
- ❌ NÃO crie testes unitários do próprio projeto (foco é o exemplo funcional)
- ❌ NÃO use frameworks como Spring, Quarkus, Micronaut
- ❌ NÃO hardcode API keys no código
- ❌ NÃO ignore tratamento de erros HTTP

---

## 🎯 CRITÉRIOS DE ACEITE

O projeto está pronto quando:

- [ ] Compila com `javac -d out $(find src -name "*.java")` sem erros
- [ ] Roda com Ollama local sem nenhuma configuração (apenas `ollama serve`)
- [ ] Roda com OpenAI/Gemini apenas setando variáveis de ambiente
- [ ] Trocar de provedor é uma linha de código (`LLMProvider.OLLAMA → OPENAI`)
- [ ] Os 3 exemplos no `Main.java` produzem saídas úteis e formatadas
- [ ] O código é legível, modular e segue boas práticas Java 21

---

## 📤 FORMATO DE ENTREGA

Entregue na seguinte ordem:

1. **Árvore de arquivos** completa
2. **Código-fonte** de cada arquivo (com caminho no topo, ex: `// src/.../Main.java`)
3. **README.md** completo
4. **Instruções de execução** passo a passo
5. **Explicação breve** das decisões arquiteturais (máx. 200 palavras)

Use blocos de código markdown com a linguagem correta (` ```java `, ` ```bash `, ` ```markdown `).

---

## 💡 CONTEXTO ADICIONAL (PERSONALIZE)

> ⚠️ **Substitua os valores abaixo antes de enviar o prompt ao LLM.**

| Campo                              | Valor                                                                  |
| ---------------------------------- | ---------------------------------------------------------------------- |
| **Meu nível**                      | `{{iniciante \| intermediário \| avançado}}`                           |
| **Caso de uso real**               | `{{chatbot interno \| revisão de código \| análise de logs \| outro}}` |
| **Provedor preferido para testes** | `{{Ollama \| OpenAI \| Gemini}}`                                       |
| **Idioma dos prompts**             | `{{português \| inglês \| ambos}}`                                     |
| **Domínio da aplicação**           | `{{e-commerce \| saúde \| financeiro \| genérico}}`                    |

### Exemplo preenchido:

```
- Meu nível: intermediário
- Caso de uso real: análise de logs de produção
- Provedor preferido: Ollama (quero testar localmente)
- Idioma dos prompts: português
- Domínio da aplicação: e-commerce
```

---

## 🧭 COMO USAR ESTE PROMPT

### 1. Personalize as variáveis

Substitua os `{{placeholders}}` na tabela acima com seu contexto real.

### 2. Envie em partes se o LLM truncar

Se a resposta ficar incompleta, diga:

> "Continue exatamente de onde parou, mantendo o mesmo formato."

### 3. Itere para melhorar

Depois da primeira versão, peça refinamentos:

- _"Adicione suporte a streaming SSE no OllamaClient"_
- _"Crie um PromptLibrary com 10 prompts prontos para DevOps"_
- _"Adicione retry com backoff exponencial nos clientes"_
- _"Converta para usar Spring AI ao invés de HttpClient puro"_

### 4. Valide o resultado

Compile e rode antes de aceitar:

```bash
javac -d out $(find src -name "*.java")
java -cp out com.exemplo.llm.Main
```

---

## 🎁 BÔNUS: PROMPT DE FOLLOW-UP

Depois que o projeto base estiver pronto, use este prompt para evoluir:

```markdown
O projeto base está funcionando. Agora evolua ele adicionando:

1. **Streaming SSE**: modifique a interface para `Stream<String> chatStream(...)`
   usando `HttpClient` com `BodyHandlers.ofLines()`
2. **Retry com backoff**: trate HTTP 429 (rate limit) com 3 tentativas
3. **Logging estruturado**: adicione logs com `System.Logger` (java.lang.System.Logger)
4. **Métricas**: conte tokens usados e tempo de resposta
5. **RAG básico**: adicione um `DocumentStore` que carrega arquivos .txt e os
   injeta no prompt como contexto

Mantenha a compatibilidade com o código existente. Mostre apenas os arquivos
modificados e os novos.
```
