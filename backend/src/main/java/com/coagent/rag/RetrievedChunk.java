package com.coagent.rag;

/** 检索命中的知识块 */
public record RetrievedChunk(String chunkId, String docName, String text, double score) {
}
