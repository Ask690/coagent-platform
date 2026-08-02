package com.coagent.rag;

import com.coagent.rag.IndexStore.Chunk;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 向量检索实现（embedding-mode=vector 时启用）。
 *
 * <p>知识块入库时惰性向量化并缓存；检索时对 query 向量化，按余弦相似度排序取 topK。
 * 索引内容版本变化（IndexStore.version）时自动重建缓存。
 *
 * <p>默认使用 {@link MockEmbeddingModel}（离线可跑）；接入真实 Embedding 服务后
 * 只要存在其它 EmbeddingModel Bean，即自动切换，业务代码零改动。
 */
@Component
@ConditionalOnProperty(name = "coagent.rag.embedding-mode", havingValue = "vector")
public class VectorRetriever implements Retriever {

    private record ChunkVec(String id, String docName, String text, float[] vec) {}

    private static final Logger log = LoggerFactory.getLogger(VectorRetriever.class);

    private final IndexStore indexStore;
    private final EmbeddingModel embeddingModel;

    private volatile long cachedVersion = -1;
    private volatile List<ChunkVec> cache = List.of();

    public VectorRetriever(IndexStore indexStore, EmbeddingModel embeddingModel) {
        this.indexStore = indexStore;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    void logActive() {
        log.info("检索器已启用：VectorRetriever（embedding-model={}, dims={}）",
                embeddingModel.getClass().getSimpleName(), embeddingModel.dimensions());
    }

    @Override
    public List<RetrievedChunk> retrieve(String query, int topK) {
        List<Chunk> chunks = indexStore.all();
        if (chunks.isEmpty()) {
            return List.of();
        }
        ensureCache(chunks);

        float[] queryVec = embeddingModel.embed(query);
        return cache.stream()
                .map(cv -> new RetrievedChunk(cv.id(), cv.docName(), cv.text(), cosine(queryVec, cv.vec())))
                .filter(r -> r.score() > 0.2)
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(Math.max(topK, 1))
                .toList();
    }

    private synchronized void ensureCache(List<Chunk> chunks) {
        long v = indexStore.version();
        if (v == cachedVersion) {
            return;
        }
        cache = chunks.stream()
                .map(c -> new ChunkVec(c.id(), c.docName(), c.text(), embeddingModel.embed(c.text())))
                .toList();
        cachedVersion = v;
    }

    private double cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
