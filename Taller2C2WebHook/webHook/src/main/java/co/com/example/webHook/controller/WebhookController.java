package co.com.example.webHook.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

	@PostMapping
	public ResponseEntity<String> receiveMessage(@RequestBody String requestBody) {
		// Procesa el mensaje recibido (puedes agregar lógica según tus necesidades)
		System.out.println("Mensaje recibido: " + requestBody);

		// Genera una respuesta para devolver al orquestador
		String responseMessage = "Webhook recibio el mensaje correctamente";

		return ResponseEntity.ok(responseMessage);
	}
}
