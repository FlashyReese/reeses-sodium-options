package me.flashyreese.mods.reeses_sodium_options.client.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SearchSession<T> {
    private final SearchIndexContext<T> index;
    private final String query;

    SearchSession(SearchIndexContext<T> index, String query) {
        this.index = index;
        this.query = query == null ? "" : query;
    }

    public List<SearchResult<T>> results() {
        String normalizedQuery = this.index.normalizer().normalize(this.query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        List<String> queryGrams = this.index.ngramGenerator().generate(normalizedQuery);
        if (queryGrams.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> queryTermCounts = new HashMap<>();
        for (String gram : queryGrams) {
            queryTermCounts.merge(gram, 1, Integer::sum);
        }
        double queryWeightSum = 0.0;
        for (Map.Entry<String, Integer> entry : queryTermCounts.entrySet()) {
            Double idf = this.index.idfWeights().get(entry.getKey());
            if (idf != null) {
                queryWeightSum += idf * entry.getValue();
            }
        }
        if (queryWeightSum == 0.0) {
            return List.of();
        }
        int totalDocs = this.index.size();
        boolean[] seen = new boolean[totalDocs];
        IntArrayList candidates = new IntArrayList();
        for (String gram : queryTermCounts.keySet()) {
            IntArrayList postings = this.index.invertedIndex().get(gram);
            if (postings == null) {
                continue;
            }
            for (int docId : postings) {
                if (!seen[docId]) {
                    seen[docId] = true;
                    candidates.add(docId);
                }
            }
        }
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<SearchResult<T>> results = new ArrayList<>();
        for (int docId : candidates) {
            String normalizedDoc = this.index.normalizedTexts().get(docId);
            if (normalizedDoc.isEmpty()) {
                continue;
            }
            Map<String, Integer> docTerms = this.index.documentTermCounts().get(docId);
            double overlap = 0.0;
            for (Map.Entry<String, Integer> entry : queryTermCounts.entrySet()) {
                Integer docCount = docTerms.get(entry.getKey());
                if (docCount != null) {
                    double idf = this.index.idfWeights().getOrDefault(entry.getKey(), 0.0);
                    overlap += idf * Math.min(entry.getValue(), docCount);
                }
            }
            double score = overlap / queryWeightSum;
            score = applyBoosts(score, normalizedQuery, normalizedDoc);
            if (score >= this.index.minScore()) {
                results.add(new SearchResult<>(this.index.items().get(docId), score, docId));
            }
        }
        if (results.isEmpty()) {
            return List.of();
        }
        results.sort(Comparator.comparingDouble(SearchResult<T>::score).reversed()
                .thenComparingInt(SearchResult::documentId));
        if (this.index.rerankWithEditDistance()) {
            rerank(results, normalizedQuery);
        }
        if (results.size() > this.index.maxResults()) {
            return new ArrayList<>(results.subList(0, this.index.maxResults()));
        }
        return results;
    }

    private double applyBoosts(double baseScore, String query, String candidate) {
        double score = baseScore;
        if (candidate.equals(query)) {
            score += 0.6;
        } else if (candidate.startsWith(query)) {
            score += 0.3;
        } else if (candidate.contains(query)) {
            score += 0.15;
        }
        int lengthDiff = Math.abs(candidate.length() - query.length());
        int maxLength = Math.max(1, Math.max(candidate.length(), query.length()));
        score -= 0.05 * (lengthDiff / (double) maxLength);
        return score;
    }

    private void rerank(List<SearchResult<T>> results, String normalizedQuery) {
        int limit = Math.min(this.index.rerankLimit(), results.size());
        if (limit <= 1) {
            return;
        }
        List<SearchResult<T>> subset = new ArrayList<>(results.subList(0, limit));
        subset.sort(Comparator.comparingDouble((SearchResult<T> result) -> {
            String candidate = this.index.normalizedTexts().get(result.documentId());
            return -adjustedScore(result.score(), normalizedQuery, candidate);
        }).thenComparingInt(SearchResult::documentId));
        for (int i = 0; i < limit; i++) {
            results.set(i, subset.get(i));
        }
    }

    private double adjustedScore(double base, String query, String candidate) {
        double distance = normalizedLevenshtein(query, candidate);
        return base + this.index.rerankWeight() * (1.0 - distance);
    }

    private double normalizedLevenshtein(String a, String b) {
        int[] aPoints = a.codePoints().toArray();
        int[] bPoints = b.codePoints().toArray();
        int aLength = aPoints.length;
        int bLength = bPoints.length;
        if (aLength == 0 && bLength == 0) {
            return 0.0;
        }
        int[] prev = new int[bLength + 1];
        int[] curr = new int[bLength + 1];
        for (int j = 0; j <= bLength; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= aLength; i++) {
            curr[0] = i;
            for (int j = 1; j <= bLength; j++) {
                int cost = aPoints[i - 1] == bPoints[j - 1] ? 0 : 1;
                int deletion = prev[j] + 1;
                int insertion = curr[j - 1] + 1;
                int substitution = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(deletion, insertion), substitution);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        int distance = prev[bLength];
        int maxLength = Math.max(aLength, bLength);
        return distance / (double) maxLength;
    }
}