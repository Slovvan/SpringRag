package com.example.springrag.Rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public class InMemoryVectorStore {
    public static class Entry {
        public final String text;
        public final String source;
        public final float[] embedding;
        public Entry(String text, String source, float[] embedding) {
            this.text = text;
            this.source = source;
            this.embedding = embedding;
        }
    }
    private final List<Entry> entries = new ArrayList<>();
    public synchronized void add(String text, String source, float[]
            embedding) {
        entries.add(new Entry(text, source, embedding));
    }
    public synchronized List<Entry> topK(float[] query, int k) {
        return entries.stream()
                .map(e-> new Scored(e, cosine(query, e.embedding)))
                .sorted(Comparator.comparingDouble((Scored s)->-s.score))
                .limit(k)
                .map(s-> s.entry)
                .toList();
    }
    private static class Scored {
        final Entry entry; final double score;
        Scored(Entry e, double s){this.entry=e;this.score=s;}
    }
    private static double cosine(float[] a, float[] b) {
        double dot=0,na=0,nb=0;
        int n = Math.min(a.length, b.length);
        for(int i=0;i<n;i++){ dot += a[i]*b[i]; na += a[i]*a[i]; nb +=
                b[i]*b[i]; }
        if (na==0 || nb==0) return 0;
        return dot / (Math.sqrt(na)*Math.sqrt(nb));
    }
}
