package co.com.example.webflux.controller;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import reactor.core.publisher.Mono;

@RestController
public class StepController {

    private WebClient webClient;

    // Constructor para inyectar WebClient.Builder correctamente
    public StepController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8083").build();
    }

    @PostMapping("/execute")
    public Mono<ResponseEntity<String>> orchestrateServices() {
        // Cuerpo de la solicitud en el formato esperado por los servicios
        String requestBody = """
        {
            "data": [
                {
                    "header": {
                        "id": "12345",
                        "type": "TestGiraffeRefrigerator"
                    }
                }
            ]
        }
        """;

        // Llamada a Service 1 con retry y circuit breaker
        Mono<String> service1 = callService1(requestBody)
                .map(this::extractAnswer)
                .onErrorResume(e -> Mono.just("Service 1 unavailable: " + e.getMessage()));

        // Llamada a Service 2 con retry y circuit breaker
        Mono<String> service2 = callService2(requestBody)
                .map(this::extractAnswer)
                .onErrorResume(e -> Mono.just("Service 2 unavailable: " + e.getMessage()));

        // Llamada a Service 3 con retry y circuit breaker
        Mono<String> service3 = callService3(requestBody)
                .map(this::extractAnswer)
                .onErrorResume(e -> Mono.just("Service 3 unavailable: " + e.getMessage()));

        // Llamada al Webhook con retry y circuit breaker
        Mono<String> webhookResponse = callWebhook(requestBody)
                .onErrorResume(e -> Mono.just("Webhook unavailable: " + e.getMessage()));

        // Combinamos todas las respuestas
        return Mono.zip(service1, service2, service3, webhookResponse)
                .map(tuple -> {
                    String service1Response = tuple.getT1();
                    String service2Response = tuple.getT2();
                    String service3Response = tuple.getT3();
                    String webhookResponseStr = tuple.getT4();

                    // Concatenar todas las respuestas de los servicios y webhook
                    return ResponseEntity.ok(
                            service1Response + "\n" +
                                    service2Response + "\n" +
                                    service3Response + "\n" +
                                    webhookResponseStr);
                })
                .onErrorResume(e -> {
                    // Si hay un fallo en algún punto que no se pudo manejar
                    return Mono.just(ResponseEntity.status(503)
                            .body("Some services are currently unavailable. Please try again later."));
                });
    }

    @CircuitBreaker(name = "service1", fallbackMethod = "fallbackService1")
    @Retry(name = "service1")
    private Mono<String> callService1(String requestBody) {
        return webClient.post()
                .uri("http://localhost:8080/getStep")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class);
    }

    @CircuitBreaker(name = "service2", fallbackMethod = "fallbackService2")
    @Retry(name = "service2")
    private Mono<String> callService2(String requestBody) {
        return webClient.post()
                .uri("http://localhost:7070/getStep")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class);
    }

    @CircuitBreaker(name = "service3", fallbackMethod = "fallbackService3")
    @Retry(name = "service3")
    private Mono<String> callService3(String requestBody) {
        return webClient.post()
                .uri("http://localhost:9090/getStep")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class);
    }

    @CircuitBreaker(name = "webhook", fallbackMethod = "fallbackWebhook")
    @Retry(name = "webhook")
    private Mono<String> callWebhook(String requestBody) {
        return webClient.post()
                .uri("http://localhost:8085/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class);
    }

    // Métodos de fallback para manejar el caso donde el Circuit Breaker está abierto
    private Mono<String> fallbackService1(String requestBody, Throwable throwable) {
        return Mono.just("Service 1 Circuit is open: " + throwable.getMessage());
    }

    private Mono<String> fallbackService2(String requestBody, Throwable throwable) {
        return Mono.just("Service 2 Circuit is open: " + throwable.getMessage());
    }

    private Mono<String> fallbackService3(String requestBody, Throwable throwable) {
        return Mono.just("Service 3 Circuit is open: " + throwable.getMessage());
    }

    private Mono<String> fallbackWebhook(String requestBody, Throwable throwable) {
        return Mono.just("Webhook Circuit is open: " + throwable.getMessage());
    }

    // Método para extraer el campo "answer" de las respuestas JSON
    private String extractAnswer(String jsonResponse) {
        try {
            // Parsear el JSON y extraer el campo "answer"
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            return rootNode.get(0)
                    .get("data")
                    .get(0)
                    .get("answer")
                    .asText();
        } catch (Exception e) {
            return "Error extracting answer: " + e.getMessage();
        }
    }

}
