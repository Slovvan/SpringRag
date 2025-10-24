package com.example.springrag.Rag;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class RagService {
    private final MistralClient mistral;
    private final InMemoryVectorStore store = new InMemoryVectorStore();
    public RagService(MistralClient mistral) {
        this.mistral = mistral;
    }
    public void ingest(String text, String source) {
        if (!mistral.isConfigured()) {
            throw new RuntimeException("Mistral API key not configured");
        }
        float[] embedding = mistral.embed(text);
        store.add(text, source, embedding);
    }
    public Answer ask(String question, int topK) {
        if (!mistral.isConfigured()) {
            throw new RuntimeException("Mistral API key not configured");
        }
        float[] q = mistral.embed(question);
        List<InMemoryVectorStore.Entry> hits = store.topK(q, Math.max(1,
                topK));
        String context = hits.stream()
                .map(e-> "[Source: " + e.source + "]\n" + e.text)
                .collect(Collectors.joining("\n\n---\n\n"));
        String system = "Tu es un assistant qui répond uniquement en te basant sur le CONTEXTE fourni. Si l'information n'est pas dans le contexte, réponds: 'Je ne sais pas.' Réponds en français.";
        String user = "Question: " + question + "\n\nCONTEXTE:\n" +
                context;
        String content = mistral.chat(system, user);
        return new Answer(content, hits.stream().map(e->
                e.source).toList());
    }
    public record Answer(String answer, List<String> sources) {}
}