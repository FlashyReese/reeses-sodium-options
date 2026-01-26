package me.flashyreese.mods.reeses_sodium_options.client.search;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class SearchIndex<T> implements SearchIndexContext<T> {
    private final List<T> items;
    private final List<String> normalizedTexts;
    private final List<Map<String, Integer>> documentTermCounts;
    private final Map<String, IntArrayList> invertedIndex;
    private final Map<String, Double> idfWeights;
    private final SearchNormalizer normalizer;
    private final NgramGenerator ngramGenerator;
    private final int maxResults;
    private final double minScore;
    private final boolean rerankWithEditDistance;
    private final int rerankLimit;
    private final double rerankWeight;

    private SearchIndex(Builder<T> builder) {
        this.normalizer = new SearchNormalizer(builder.foldDiacritics);
        this.ngramGenerator = new NgramGenerator(2, 3, true);
        this.items = new ArrayList<>(builder.items.size());
        this.normalizedTexts = new ArrayList<>(builder.items.size());
        this.documentTermCounts = new ArrayList<>(builder.items.size());
        this.invertedIndex = new HashMap<>();
        this.idfWeights = new HashMap<>();
        this.maxResults = builder.maxResults;
        this.minScore = builder.minScore;
        this.rerankWithEditDistance = builder.rerankWithEditDistance;
        this.rerankLimit = builder.rerankLimit;
        this.rerankWeight = builder.rerankWeight;
        build(builder.items, builder.extractSearchableText);
    }

    public static <T> Builder<T> builder(Function<T, String> extractSearchableText) {
        return new Builder<>(extractSearchableText);
    }

    public SearchSession<T> newSession(String query) {
        return new SearchSession<>(this, query);
    }

    @Override
    public List<T> items() {
        return this.items;
    }

    @Override
    public List<String> normalizedTexts() {
        return this.normalizedTexts;
    }

    @Override
    public List<Map<String, Integer>> documentTermCounts() {
        return this.documentTermCounts;
    }

    @Override
    public Map<String, IntArrayList> invertedIndex() {
        return this.invertedIndex;
    }

    @Override
    public Map<String, Double> idfWeights() {
        return this.idfWeights;
    }

    @Override
    public SearchNormalizer normalizer() {
        return this.normalizer;
    }

    @Override
    public NgramGenerator ngramGenerator() {
        return this.ngramGenerator;
    }

    @Override
    public int maxResults() {
        return this.maxResults;
    }

    @Override
    public double minScore() {
        return this.minScore;
    }

    @Override
    public boolean rerankWithEditDistance() {
        return this.rerankWithEditDistance;
    }

    @Override
    public int rerankLimit() {
        return this.rerankLimit;
    }

    @Override
    public double rerankWeight() {
        return this.rerankWeight;
    }

    public int size() {
        return this.items.size();
    }

    private void build(List<T> sourceItems, Function<T, String> extractSearchableText) {
        for (T item : sourceItems) {
            String rawText = extractSearchableText.apply(item);
            String normalized = this.normalizer.normalize(rawText);
            this.items.add(item);
            this.normalizedTexts.add(normalized);
            Map<String, Integer> termCounts = new HashMap<>();
            if (!normalized.isEmpty()) {
                List<String> grams = this.ngramGenerator.generate(normalized);
                Set<String> unique = new HashSet<>(grams.size());
                for (String gram : grams) {
                    termCounts.merge(gram, 1, Integer::sum);
                    if (unique.add(gram)) {
                        this.invertedIndex.computeIfAbsent(gram, key -> new IntArrayList()).add(this.items.size() - 1);
                    }
                }
                for (String gram : unique) {
                    this.idfWeights.merge(gram, 1.0, Double::sum);
                }
            }
            this.documentTermCounts.add(termCounts);
        }
        int totalDocs = Math.max(1, this.items.size());
        for (Map.Entry<String, Double> entry : this.idfWeights.entrySet()) {
            double df = entry.getValue();
            double idf = Math.log((totalDocs + 1.0) / (df + 1.0)) + 1.0;
            entry.setValue(idf);
        }
    }

    public static final class Builder<T> {
        private final Function<T, String> extractSearchableText;
        private final List<T> items = new ArrayList<>();
        private boolean foldDiacritics;
        private int maxResults = 10;
        private double minScore = 0.15;
        private boolean rerankWithEditDistance = true;
        private int rerankLimit = 50;
        private double rerankWeight = 0.1;

        private Builder(Function<T, String> extractSearchableText) {
            this.extractSearchableText = Objects.requireNonNull(extractSearchableText, "extractSearchableText");
        }

        public Builder<T> add(T item) {
            this.items.add(item);
            return this;
        }

        public Builder<T> addAll(List<T> items) {
            this.items.addAll(items);
            return this;
        }

        public Builder<T> foldDiacritics(boolean foldDiacritics) {
            this.foldDiacritics = foldDiacritics;
            return this;
        }

        public Builder<T> maxResults(int maxResults) {
            this.maxResults = Math.max(1, maxResults);
            return this;
        }

        public Builder<T> minScore(double minScore) {
            this.minScore = Math.max(0.0, minScore);
            return this;
        }

        public Builder<T> rerankWithEditDistance(boolean rerankWithEditDistance) {
            this.rerankWithEditDistance = rerankWithEditDistance;
            return this;
        }

        public Builder<T> rerankLimit(int rerankLimit) {
            this.rerankLimit = Math.max(1, rerankLimit);
            return this;
        }

        public Builder<T> rerankWeight(double rerankWeight) {
            this.rerankWeight = Math.max(0.0, rerankWeight);
            return this;
        }

        public SearchIndex<T> build() {
            return new SearchIndex<>(this);
        }
    }
}