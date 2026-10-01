# llm-java-integration

Integração de **OpenAI**, **Google Gemini** e **Ollama** em **Java 21 puro** (sem Spring, sem Jackson, sem Maven), com uma interface única (`LLMClient`) e um motor de **Prompt Engineering** dinâmico: templates `{{variavel}}` e uma biblioteca de prompts prontos que aplicam Role Assignment, Chain-of-Thought, Few-Shot/Structured Output e Guardrails. O `Main` mostra três casos reais de um e-commerce: code review, diagnóstico de bug a partir de stack trace e geração de testes JUnit 5. Trocar de provedor é uma linha de código ou um argumento de linha de comando.

## Pré-requisitos

| Item | Observação |
|------|------------|
| **JDK 21+** | `java -version` deve mostrar 21 ou superior |
| **Ollama** (opcional) | Para rodar modelos locais sem chave: <https://ollama.com/download> |
| **OPENAI_API_KEY** (opcional) | Só para usar a OpenAI |
| **GEMINI_API_KEY** (opcional) | Só para usar o Gemini ([Google AI Studio](https://aistudio.google.com/apikey)) |

## Como executar

### 1. Compilar

```bash
javac -d out $(find src -name "*.java")
```

> No Windows (PowerShell):
> `javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName`

### 2. Ollama (local, sem chave): padrão

```bash
ollama serve                       # em outro terminal (o app desktop já faz isso)
ollama pull llama3.2
java -cp out com.exemplo.llm.Main
```

Para usar um modelo focado em código:

```bash
ollama pull qwen2.5-coder
LLM_MODEL=qwen2.5-coder java -cp out com.exemplo.llm.Main
```

### 3. OpenAI

```bash
export OPENAI_API_KEY="sk-..."
java -cp out com.exemplo.llm.Main openai
LLM_MODEL=gpt-4o java -cp out com.exemplo.llm.Main openai   # outro modelo
```

### 4. Gemini

```bash
export GEMINI_API_KEY="..."
java -cp out com.exemplo.llm.Main gemini
LLM_MODEL=gemini-1.5-pro java -cp out com.exemplo.llm.Main gemini
```

### PowerShell (Windows)

```powershell
$env:OPENAI_API_KEY = "sk-..."
java "-Dstdout.encoding=UTF-8" -cp out com.exemplo.llm.Main openai
```

> Se acentos aparecerem quebrados no console do Windows, rode `chcp 65001` antes e passe `-Dstdout.encoding=UTF-8` para o `java`.

### Variáveis de ambiente

| Variável | Uso | Padrão |
|----------|-----|--------|
| `OPENAI_API_KEY` | Chave da OpenAI | (obrigatória para OpenAI) |
| `GEMINI_API_KEY` | Chave do Gemini | (obrigatória para Gemini) |
| `LLM_MODEL` | Sobrescreve o modelo | `gpt-4o-mini` / `gemini-2.0-flash` / `llama3.2` |
| `OLLAMA_BASE_URL` | Endereço do Ollama | `http://localhost:11434` |

## Estrutura do projeto

```
src/main/java/com/exemplo/llm/
├── model/
│   ├── LLMProvider.java          # Enum: URL base, env var da chave, modelos conhecidos
│   ├── ChatMessage.java          # Record (role, content) + factory methods
│   └── ChatRequest.java          # Record imutável + Builder com validações
├── prompt/
│   ├── PromptTemplate.java       # Motor de templates {{variavel}} (regex)
│   └── PromptLibrary.java        # Prompts prontos: code review, bug, testes, logs
├── client/
│   ├── LLMClient.java            # Interface Strategy
│   ├── AbstractLLMClient.java    # Template Method: envio HTTP + tratamento de erros
│   ├── LLMException.java         # Hierarquia selada de erros (401, 429, 5xx, ...)
│   ├── OpenAIClient.java         # Chat Completions
│   ├── GeminiClient.java         # generateContent (key na URL)
│   └── OllamaClient.java         # /api/chat local, sem autenticação
├── config/
│   └── LLMConfig.java            # Factory method + variáveis de ambiente
├── util/
│   └── JsonUtils.java            # Escape e extração de JSON sem bibliotecas
└── Main.java                     # 3 exemplos de uso
```

## Técnicas de Prompt Engineering

| Técnica | Onde | Como foi aplicada |
|---------|------|-------------------|
| **Role Assignment** | todos os prompts de sistema | Persona especialista com experiência e prioridades explícitas (revisor sênior, especialista em diagnóstico, engenheiro de QA, SRE) |
| **Chain-of-Thought** | `PromptLibrary.bugAnalysis()` | 7 etapas obrigatórias: leitura → localização → hipóteses → verificação → causa raiz → correção → prevenção |
| **Few-Shot** | `codeReview()`, `unitTests()` | Linha de exemplo na tabela de problemas; método de teste modelo com nomenclatura e `@DisplayName` |
| **Structured Output** | `codeReview()`, `unitTests()`, `logAnalysis()` | Markdown com seções fixas; somente um bloco ```java; JSON com schema |
| **Guardrails** | fragmento `GUARDRAILS` composto em todos | Idioma, escopo, não inventar fatos, mascarar segredos/dados pessoais |
| **Delimitação de entrada** | `<entrada>...</entrada>` | Dados do usuário tratados como dado, nunca como instrução (mitiga prompt injection) |
| **Controle de amostragem** | `Main` | `temperature` 0.2 (review), 0.3 (bug), **0.0** (testes, determinístico); `maxTokens` por caso |

## Como estender

### Novo provedor (ex.: Anthropic Claude)

1. Adicione a constante em `LLMProvider` (URL base, variável da chave, modelos).
2. Crie `ClaudeClient extends AbstractLLMClient` implementando apenas:
   - `provider()`
   - `buildHttpRequest(request, model)`: corpo e cabeçalhos do provedor
   - `extractContent(body)`: ex.: `JsonUtils.findString(body, "text", "content")`
3. Adicione o `case` no `switch` de `LLMConfig.createClient`. O compilador acusa o `case` faltante, porque o `switch` sobre o enum é exaustivo.

Tratamento de status HTTP, timeouts e falhas de conexão já vêm da classe base.

### Novo prompt

```java
// em PromptLibrary
private static final PromptTemplate SQL_EXPERT = PromptTemplate.of("""
        Você é um DBA especialista em {{banco}} ...""");
private static final PromptTemplate SQL_TASK = PromptTemplate.of("""
        Otimize a consulta abaixo e explique o plano de execução.
        <entrada>
        {{sql}}
        </entrada>""");

public static Prompt sqlOptimization() {
    return new Prompt("sql-optimization", SQL_EXPERT.append(GUARDRAILS), SQL_TASK);
}
```

Uso:

```java
var messages = PromptLibrary.sqlOptimization().render(Map.of("banco", "PostgreSQL", "sql", query));
var request = ChatRequest.builder().messages(messages).temperature(0.2).maxTokens(1500).build();
String resposta = LLMConfig.createClient(LLMProvider.OLLAMA).chat(request);
```

`PromptTemplate.render` falha com `IllegalArgumentException` listando as variáveis sem valor. `partial(...)` preenche só parte delas, para compor o prompt em etapas.

## Próximos passos

- **Streaming (SSE)**: `Stream<String> chatStream(...)` com `HttpResponse.BodyHandlers.ofLines()`.
- **Resiliência**: retry com backoff exponencial para HTTP 429/5xx.
- **Observabilidade**: `System.Logger`, contagem de tokens (`usage`) e latência por chamada.
- **RAG**: um `DocumentStore` com embeddings e busca vetorial para injetar contexto nos prompts.
- **Spring AI**: migrar para `ChatClient`/`PromptTemplate` do Spring AI quando o projeto for para um ambiente Spring Boot.
- **Saída tipada**: validar o JSON de `logAnalysis()` e mapear para records.
