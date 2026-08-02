package com.coagent.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切块：按段落贪心合并到目标块大小，块尾保留 overlap 重叠，避免语义断点。
 */
public final class Chunker {

    private Chunker() {}

    public static List<String> split(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        StringBuilder cur = new StringBuilder();
        for (String para : text.split("\\n")) {
            String p = para.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (cur.length() > 0 && cur.length() + p.length() > chunkSize) {
                chunks.add(cur.toString());
                cur.setLength(0);
                // 上一块尾部作为下一块开头，保持语义连贯
                if (overlap > 0 && !chunks.isEmpty()) {
                    String last = chunks.get(chunks.size() - 1);
                    if (last.length() > overlap) {
                        cur.append(last, last.length() - overlap, last.length());
                    } else {
                        cur.append(last);
                    }
                }
            }
            cur.append(p).append('\n');
        }
        if (!cur.isEmpty()) {
            chunks.add(cur.toString());
        }
        return chunks;
    }
}
