package me.flashyreese.mods.reeses_sodium_options.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class StringUtils {

    // Normalize text for localization handling
    public static String normalizeText(String text) {
        text = text.toLowerCase();
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("\\p{M}", "");
        return text;
    }

    // Levenshtein distance algorithm for approximate matching
    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    // Boyer-Moore exact matching algorithm
    public static int boyerMooreSearch(String text, String pattern) {
        int m = pattern.length();
        int n = text.length();
        if (m == 0) return 0;

        int[] skip = new int[256];
        for (int k = 0; k < 256; k++) {
            skip[k] = m;
        }
        for (int k = 0; k < m - 1; k++) {
            skip[pattern.charAt(k)] = m - k - 1;
        }

        int k = m - 1;
        while (k < n) {
            int j = m - 1;
            int i = k;
            while (j >= 0 && text.charAt(i) == pattern.charAt(j)) {
                j--;
                i--;
            }
            if (j == -1) {
                return i + 1;
            }
            int index = text.charAt(k);
            if (index >= skip.length) {
                break;
            }
            k += skip[index];
        }
        return -1;
    }

    // Combined search function
    public static <T> List<T> searchElements(Iterable<T> elements, String query, Function<T, String> extractSearchableText) {
        String normalizedQuery = normalizeText(query);
        List<T> exactMatches = new ArrayList<>();
        List<T> approxMatches = new ArrayList<>();

        for (T element : elements) {
            String normalizedName = normalizeText(extractSearchableText.apply(element));

            if (boyerMooreSearch(normalizedName, normalizedQuery) != -1) {
                exactMatches.add(element);
            } else {
                int nameDistance = levenshteinDistance(normalizedName, normalizedQuery);
                if (nameDistance < normalizedQuery.length() / 2) {
                    approxMatches.add(element);
                }
            }
        }

        // Sort approximate matches by Levenshtein distance
        // For future use
        /*approxMatches.sort(Comparator.comparingInt(entry ->
                levenshteinDistance(normalizeText(extractSearchableText.apply(entry)), normalizedQuery)
        ));*/

        List<T> results = new ArrayList<>(exactMatches);
        results.addAll(approxMatches);
        return results;
    }
}