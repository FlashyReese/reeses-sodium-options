package me.flashyreese.mods.reeses_sodium_options.client.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NgramGenerator {

    // Non-printing sentinels (won't collide with user text)
    private static final int START = 0x0002; // STX
    private static final int END = 0x0003; // ETX

    private final int minGram;
    private final int maxGram;
    private final boolean tokenizeOnLetterDigit;

    public NgramGenerator(int minGram, int maxGram,
                          boolean tokenizeOnLetterDigit) {

        if (minGram < 1 || maxGram < minGram) {
            throw new IllegalArgumentException("Invalid gram range");
        }

        this.minGram = minGram;
        this.maxGram = maxGram;
        this.tokenizeOnLetterDigit = tokenizeOnLetterDigit;
    }

    private static boolean allBoundary(int[] arr, int off, int n) {
        for (int i = 0; i < n; i++) {
            int cp = arr[off + i];
            if (cp != START && cp != END) return false;
        }
        return true;
    }

    public List<String> generate(String text) {
        if (text == null || text.isEmpty()) return List.of();

        String norm = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();

        if (norm.isEmpty()) return List.of();

        List<String> out = new ArrayList<>(Math.max(8, norm.length() * 2));

        if (!this.tokenizeOnLetterDigit) {
            addTokenGrams(norm, out);
            return out;
        }


        StringBuilder token = new StringBuilder();
        for (int i = 0; i < norm.length(); ) {
            int cp = norm.codePointAt(i);
            i += Character.charCount(cp);

            if (isTokenChar(cp)) {
                token.appendCodePoint(cp);
            } else {
                flushToken(token, out);
            }
        }
        flushToken(token, out);

        return out;
    }

    private boolean isTokenChar(int cp) {
        // If not tokenizing, whole string is one token
        if (!this.tokenizeOnLetterDigit) return true;

        // Normal words + CJK (Chinese/Japanese/Korean letters count as letters)
        if (Character.isLetterOrDigit(cp)) return true;

        // Preserve Minecraft identifier punctuation inside tokens (ASCII only)
        if (cp <= 0x7F) {
            return isResourceLocationPunctuation((char) cp);
        }

        return false;
    }

    private static boolean isResourceLocationPunctuation(char c) {
        return c == '_' || c == '-' || c == '.' || c == '/' || c == ':';
    }

    private void flushToken(StringBuilder token, List<String> out) {
        if (token.isEmpty()) return;
        addTokenGrams(token.toString(), out);
        token.setLength(0);
    }

    private void addTokenGrams(String token, List<String> out) {
        int[] cps = token.codePoints().toArray();

        int startPad = Math.max(1, this.maxGram - 1);

        int[] padded = new int[startPad + cps.length + 1];
        for (int i = 0; i < startPad; i++) padded[i] = START;
        System.arraycopy(cps, 0, padded, startPad, cps.length);
        padded[padded.length - 1] = END;

        for (int n = this.minGram; n <= this.maxGram; n++) {
            if (padded.length < n) continue;

            for (int off = 0; off <= padded.length - n; off++) {
                if (allBoundary(padded, off, n)) continue;
                out.add(new String(padded, off, n));
            }
        }
    }
}
