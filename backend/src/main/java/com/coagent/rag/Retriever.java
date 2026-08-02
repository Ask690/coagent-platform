package com.coagent.rag;

import java.util.List;

/**
 * 检索器抽象。
 *
 * <p>当前默认实现为本地 BM25 词法检索（零外部依赖，离线可跑）。
 * 生产环境可平滑替换为向量检索（EmbeddingModel + VectorStore），只需新增实现：
 * <pre>
 *     &#64;Component
 *     &#64;ConditionalOnProperty(name = "coagent.rag.embedding-mode", havingValue = "vector")
 *     class VectorRetriever implements Retriever { ... }
 * </pre>
 */
public interface Retriever {

    List<RetrievedChunk> retrieve(String query, int topK);
}
