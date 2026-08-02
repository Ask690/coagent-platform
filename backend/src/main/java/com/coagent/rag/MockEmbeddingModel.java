package com.coagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地 Mock Embedding 模型：基于词袋(bag-of-words) hash 到固定维度的伪向量。
 *
 * <p>离线可跑、确定性输出，用于在没有真实 Embedding API 时完整演示向量检索链路
 * （向量化 -> 余弦相似度排序）。
 *
 * <p>启用条件：RAG 向量模式（embedding-mode=vector）且为 Mock 模式（coagent.ai.mock=true）。
 * 接入真实 Embedding 服务：设 AI_EMBEDDING_PROVIDER=openai 并配置 api-key，本类自动不装配。
 */
@Component
@ConditionalOnProperty(name = "coagent.rag.embedding-mode", havingValue = "vector")
@ConditionalOnProperty(name = "coagent.ai.mock", havingValue = "true", matchIfMissing = true)
public class MockEmbeddingModel implements EmbeddingModel {

    private static final int DIM = 256;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = request.getInstructions().stream()
                .map(text -> new Embedding(embed(text), 0))
                .toList();
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getFormattedContent());
    }

    @Override
    public float[] embed(String text) {
        // 词袋：分词 token 经 hash 映射到固定维度累加词频，再 L2 归一化
        float[] vec = new float[DIM];
        for (String token : CjkTokenizer.tokenize(text)) {
            int idx = Math.floorMod(token.hashCode(), DIM);
            vec[idx] += 1f;
        }
        double norm = 0;
        for (float v : vec) {
            norm += v * v;
        }
        if (norm > 0) {
            float inv = (float) (1.0 / Math.sqrt(norm));
            for (int i = 0; i < vec.length; i++) {
                vec[i] *= inv;
            }
        }
        return vec;
    }

    @Override
    public int dimensions() {
        return DIM;
    }
}
