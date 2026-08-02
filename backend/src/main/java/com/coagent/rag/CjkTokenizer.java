package com.coagent.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量中文分词：中文按「二元组(bigram)」切分，英文/数字按单词切分。
 *
 * <p>中文没有空格分词，bigram 是一种简单有效、零依赖的近似方案，
 * 足以支撑 BM25 检索；生产可替换为 IK/结巴等分词器。
 */
public final class CjkTokenizer {

    private CjkTokenizer() {}

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        String lower = text.toLowerCase();

        StringBuilder latin = new StringBuilder();
        // 连续中文做 bigram；中文与拉丁字符都切到 tokens
        List<Character> cjkBuffer = new ArrayList<>();

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isCjk(c)) {
                flushLatin(latin, tokens);
                cjkBuffer.add(c);
            } else if (Character.isLetterOrDigit(c)) {
                flushCjk(cjkBuffer, tokens);
                latin.append(c);
            } else {
                flushLatin(latin, tokens);
                flushCjk(cjkBuffer, tokens);
            }
        }
        flushLatin(latin, tokens);
        flushCjk(cjkBuffer, tokens);
        return tokens;
    }

    private static void flushLatin(StringBuilder buf, List<String> out) {
        if (buf.length() > 0) {
            out.add(buf.toString());
            buf.setLength(0);
        }
    }

    private static void flushCjk(List<Character> buf, List<String> out) {
        if (buf.isEmpty()) {
            return;
        }
        if (buf.size() == 1) {
            out.add(String.valueOf(buf.get(0)));
        } else {
            for (int i = 0; i < buf.size() - 1; i++) {
                out.add("" + buf.get(i) + buf.get(i + 1));
            }
            // 也保留单字，提升召回
            for (char c : buf) {
                out.add(String.valueOf(c));
            }
        }
        buf.clear();
    }

    private static boolean isCjk(char c) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || c == 0x3000; // 全角空格
    }
}
