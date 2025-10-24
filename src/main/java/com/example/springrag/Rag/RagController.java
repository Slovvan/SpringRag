package com.example.springrag.Rag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/rag")
public class RagController {
    private final RagService ragService;
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }
    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody Map<String, String> body)
    {
        String text = body.getOrDefault("text", "");
        String source = body.getOrDefault("source", "custom");
        if (text.isBlank()) return
                ResponseEntity.badRequest().body(Map.of("error", "text manquant"));
        try {
            ragService.ingest(text, source);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("401")) {
                return ResponseEntity.status(500).body(Map.of(
                        "error", "MISTRAL_API_KEY manquante ou invalide",
                        "details", msg
                ));
            }
            return ResponseEntity.status(500).body(Map.of("error",
                    "ingestion failed", "details", msg));
        }
    }
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody Map<String, Object> body) {
        String question = (String) body.getOrDefault("question", "");
        int topK = ((Number) body.getOrDefault("topK", 4)).intValue();
        if (question.isBlank()) return
                ResponseEntity.badRequest().body(Map.of("error", "question manquante"));
        try {
            var answer = ragService.ask(question, topK);
            return ResponseEntity.ok(Map.of(
                    "answer", answer.answer(),
                    "sources", answer.sources()
            ));
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("401") || msg.contains("not configured"))) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "MISTRAL_API_KEY manquante ou invalide",
                    "details", msg
            ));
        }
        if (msg != null && (msg.contains(" 429:") ||
                msg.toLowerCase().contains("capacity exceeded"))) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Capacité momentanément dépassée pour le modèle. Réessayez.",
                    "details", msg
            ));
        }
        return ResponseEntity.status(500).body(Map.of("error", "ask failed", "details", msg));
    }
}
 }
