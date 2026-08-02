package com.coagent.rag;

import com.coagent.rag.IndexStore.Chunk;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BM25 词法检索实现（默认检索器）。
 *
 * <p>公式：score(q,d) = Σ idf(t) * tf(t,d)*(k1+1) / (tf(t,d) + k1*(1 - b + b*|d|/avgdl))
 * 中文走 bigram 分词，英文走单词，零外部依赖。
 */
@Component
@ConditionalOnProperty(name = "coagent.rag.embedding-mode", havingValue = "lexical", matchIfMissing = true)
public class LexicalRetriever implements Retriever {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    private static final Logger log = LoggerFactory.getLogger(LexicalRetriever.class);

    private final IndexStore indexStore;

    public LexicalRetriever(IndexStore indexStore) {
        this.indexStore = indexStore;
    }

    @PostConstruct
    void logActive() {
        log.info("检索器已启用：LexicalRetriever（BM25 词法检索）");
    }

    @Override
    public List<RetrievedChunk> retrieve(String query, int topK) {
        List<Chunk> chunks = indexStore.all();
        if (chunks.isEmpty()) {
            return List.of();
        }
        List<String> qTerms = CjkTokenizer.tokenize(query).stream().distinct().toList();
        if (qTerms.isEmpty()) {
            return List.of();
        }

        double avgdl = chunks.stream().mapToInt(c -> c.text().length()).average().orElse(1.0);
        Map<String, Integer> docFreq = new HashMap<>();
        for (Chunk c : chunks) {
            List<String> terms = CjkTokenizer.tokenize(c.text());
            for (String t : terms.stream().distinct().toList()) {
                docFreq.merge(t, 1, Integer::sum);
            }
        }

        int n = chunks.size();
        double[] scores = new double[chunks.size()];
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            List<String> terms = CjkTokenizer.tokenize(c.text());
            Map<String, Integer> tf = new HashMap<>();
            for (String t : terms) {
                tf.merge(t, 1, Integer::sum);
            }
            double dl = c.text().length();
            double score = 0;
            for (String t : qTerms) {
                int df = docFreq.getOrDefault(t, 0);
                if (df == 0) {
                    continue;
                }
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                int count = tf.getOrDefault(t, 0);
                score += idf * (count * (K1 + 1)) / (count + K1 * (1 - B + B * dl / avgdl));
            }
            scores[i] = score;
        }

        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(i -> new RetrievedChunk(chunks.get(i).id(), chunks.get(i).docName(),
                        chunks.get(i).text(), scores[i]))
                .filter(r -> r.score() > 0)
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(Math.max(topK, 1))
                .toList();
    }
}
