package com.example.batch.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OrchestratorScheduledTask {

    private final WebClient webClient;

    public OrchestratorScheduledTask(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8083").build();
    }

    @Scheduled(fixedRate = 120000) // Cada 15 segundos (en milisegundos)
    public void callOrchestrator() {

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

        Mono<String> response = webClient.post()
                .uri("/execute") // El endpoint de tu orquestador
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

        response.subscribe(
                result -> System.out.println("Orquestador respondió: " + result),
                error -> System.err.println("Error al invocar orquestador: " + error.getMessage())
        );
    }
}
