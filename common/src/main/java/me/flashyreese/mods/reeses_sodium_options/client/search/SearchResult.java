package me.flashyreese.mods.reeses_sodium_options.client.search;

public final class SearchResult<T> {
    private final T item;
    private final double score;
    private final int documentId;

    SearchResult(T item, double score, int documentId) {
        this.item = item;
        this.score = score;
        this.documentId = documentId;
    }

    public T item() {
        return this.item;
    }

    public double score() {
        return this.score;
    }

    int documentId() {
        return this.documentId;
    }
}