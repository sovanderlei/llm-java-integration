package com.exemplo.llm;

import com.exemplo.llm.client.LLMClient;
import com.exemplo.llm.client.LLMException;
import com.exemplo.llm.config.LLMConfig;
import com.exemplo.llm.model.ChatRequest;
import com.exemplo.llm.model.LLMProvider;
import com.exemplo.llm.prompt.PromptLibrary;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Demonstra três casos reais de uso de LLM em um e-commerce: code review,
 * análise de bug e geração de testes.
 *
 * <p>Uso: {@code java -cp out com.exemplo.llm.Main [ollama|openai|gemini]}
 */
public final class Main {

    /** Trocar de provedor é uma linha: OLLAMA → OPENAI ou GEMINI. */
    private static final LLMProvider DEFAULT_PROVIDER = LLMProvider.OLLAMA;

    private static final String SEPARATOR = "=".repeat(80);

    private record Example(String title, Supplier<ChatRequest> request) {
    }

    // ---------------------------------------------------------------- entradas de exemplo

    private static final String LEGACY_CODE = """
            public class PedidoService {
                public static List pedidos = new ArrayList();

                public double calcularTotal(Pedido p) {
                    double total = 0;
                    for (int i = 0; i <= p.getItens().size(); i++) {
                        total = total + p.getItens().get(i).getPreco() * p.getItens().get(i).getQtd();
                    }
                    if (p.getCupom() != "") {
                        total = total - total * 0.1;
                    }
                    try {
                        Connection c = DriverManager.getConnection("jdbc:mysql://prod-db/loja", "root", "admin123");
                        c.createStatement().execute("UPDATE pedidos SET total=" + total + " WHERE id=" + p.getId());
                    } catch (Exception e) {}
                    pedidos.add(p);
                    return total;
                }
            }""";

    private static final String STACK_TRACE = """
            java.lang.NullPointerException: Cannot invoke "java.math.BigDecimal.multiply(java.math.BigDecimal)" \
            because the return value of "com.loja.carrinho.ItemCarrinho.getPrecoPromocional()" is null
                at com.loja.carrinho.CarrinhoService.lambda$calcularSubtotal$0(CarrinhoService.java:42)
                at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
                at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1625)
                at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
                at com.loja.carrinho.CarrinhoService.calcularSubtotal(CarrinhoService.java:44)
                at com.loja.checkout.CheckoutService.fecharPedido(CheckoutService.java:71)
                at com.loja.checkout.CheckoutController.finalizar(CheckoutController.java:88)
                at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:883)""";

    private static final String BUGGY_CODE = """
            public BigDecimal calcularSubtotal(Carrinho carrinho) {
                return carrinho.getItens().stream()
                        .map(item -> item.getPrecoPromocional()
                                .multiply(BigDecimal.valueOf(item.getQuantidade())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }""";

    private static final String METHOD_UNDER_TEST = """
            public BigDecimal calcularFrete(BigDecimal valorPedido, String cep, double pesoKg) {
                if (valorPedido == null || cep == null) {
                    throw new IllegalArgumentException("valorPedido e cep são obrigatórios");
                }
                if (pesoKg <= 0) {
                    throw new IllegalArgumentException("peso deve ser positivo");
                }
                if (!cep.matches("\\\\d{5}-?\\\\d{3}")) {
                    throw new IllegalArgumentException("CEP inválido: " + cep);
                }
                if (valorPedido.compareTo(new BigDecimal("299.00")) >= 0) {
                    return BigDecimal.ZERO; // frete grátis
                }
                BigDecimal base = cep.startsWith("0") ? new BigDecimal("15.00") : new BigDecimal("25.00");
                BigDecimal adicionalPeso = BigDecimal.valueOf(Math.max(0, pesoKg - 1))
                        .multiply(new BigDecimal("4.50"));
                return base.add(adicionalPeso).setScale(2, RoundingMode.HALF_UP);
            }""";

    private Main() {
    }

    public static void main(String[] args) {
        try {
            LLMProvider provider = args.length > 0 ? LLMProvider.fromName(args[0]) : DEFAULT_PROVIDER;
            LLMClient client = LLMConfig.createClient(provider);
            printBanner(client);
            runAll(client, examples());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Configuração inválida: " + e.getMessage());
            System.exit(1);
        }
    }

    private static List<Example> examples() {
        return List.of(
                new Example("1. Code Review — Role Assignment + Structured Output", Main::codeReviewRequest),
                new Example("2. Análise de Bug — Chain-of-Thought", Main::bugAnalysisRequest),
                new Example("3. Testes JUnit 5 — Few-Shot + Structured Output (temperature=0.0)",
                        Main::unitTestRequest));
    }

    // ---------------------------------------------------------------- requisições

    /** Baixa temperatura: revisão deve ser consistente, mas com alguma liberdade de redação. */
    private static ChatRequest codeReviewRequest() {
        var messages = PromptLibrary.codeReview().render(Map.of(
                "linguagem", "Java",
                "codigo", LEGACY_CODE));
        return ChatRequest.builder().messages(messages).temperature(0.2).maxTokens(2048).build();
    }

    /** CoT gera respostas longas: mais tokens; temperatura baixa para manter o foco em evidências. */
    private static ChatRequest bugAnalysisRequest() {
        var messages = PromptLibrary.bugAnalysis().render(Map.of(
                "contexto", "Checkout do e-commerce; falha intermitente desde a inclusão de "
                        + "produtos sem promoção ativa na campanha de Black Friday.",
                "stacktrace", STACK_TRACE,
                "codigo", BUGGY_CODE));
        return ChatRequest.builder().messages(messages).temperature(0.3).maxTokens(2048).build();
    }

    /** Código gerado deve ser determinístico e reproduzível: temperature = 0.0. */
    private static ChatRequest unitTestRequest() {
        var messages = PromptLibrary.unitTests().render(Map.of(
                "classeTeste", "FreteServiceTest",
                "classeAlvo", "FreteService",
                "codigo", METHOD_UNDER_TEST));
        return ChatRequest.builder().messages(messages).temperature(0.0).maxTokens(3000).build();
    }

    // ---------------------------------------------------------------- execução e saída

    private static void runAll(LLMClient client, List<Example> examples) {
        for (Example example : examples) {
            if (!run(client, example)) {
                System.err.println("\nExecução interrompida: corrija o problema acima e tente novamente.");
                return;
            }
        }
    }

    /** Executa um exemplo; retorna {@code false} se o erro impede os próximos. */
    private static boolean run(LLMClient client, Example example) {
        printHeader(example.title());
        try {
            long start = System.nanoTime();
            String answer = client.chat(example.request().get());
            printAnswer(answer, Duration.ofNanos(System.nanoTime() - start));
            return true;
        } catch (LLMException e) {
            System.err.println("ERRO: " + e.getMessage());
            System.err.println("DICA: " + hintFor(e));
            return !isFatal(e);
        }
    }

    /** Switch exaustivo sobre a hierarquia selada — sem {@code default}. */
    private static String hintFor(LLMException e) {
        return switch (e) {
            case LLMException.Unauthorized u ->
                    "Confira a variável de ambiente " + u.provider().apiKeyEnvVar() + ".";
            case LLMException.RateLimited r -> "Aguarde alguns segundos ou verifique a cota do seu plano.";
            case LLMException.ServerError s -> "Instabilidade no provedor; tente novamente em instantes.";
            case LLMException.ClientError c -> "Revise o nome do modelo (LLM_MODEL) e os parâmetros.";
            case LLMException.ConnectionFailed c -> "Verifique rede, URL e se o serviço está no ar.";
            case LLMException.InvalidResponse i -> "Resposta inesperada; tente aumentar maxTokens ou outro modelo.";
        };
    }

    /** Erros que se repetiriam em todos os exemplos seguintes. */
    private static boolean isFatal(LLMException e) {
        return e instanceof LLMException.Unauthorized
                || e instanceof LLMException.ConnectionFailed
                || (e instanceof LLMException.ClientError && e.statusCode() == 404);
    }

    private static void printBanner(LLMClient client) {
        System.out.println(SEPARATOR);
        System.out.printf("LLM Java Integration | Provedor: %s | Modelo: %s%n",
                client.provider(), client.defaultModel());
        System.out.println(SEPARATOR);
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println(title);
        System.out.println(SEPARATOR);
        System.out.println("Aguardando resposta do modelo...");
    }

    private static void printAnswer(String answer, Duration elapsed) {
        System.out.println();
        System.out.println(answer.strip());
        System.out.println("-".repeat(80));
        System.out.printf(Locale.ROOT, "Tempo de resposta: %.1fs%n", elapsed.toMillis() / 1000.0);
    }
}
