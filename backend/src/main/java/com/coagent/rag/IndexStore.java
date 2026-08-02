package com.coagent.rag;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存倒排索引存储：存放切分后的知识块。
 *
 * <p>纯内存实现保证零依赖可跑；数据同时持久化在 knowledge_doc 表，
 * 重启后由 DocumentService 从数据库重建索引。
 */
@Component
public class IndexStore {

    public record Chunk(String id, String docName, String text) {}

    private final List<Chunk> chunks = new CopyOnWriteArrayList<>();

    public void addAll(List<Chunk> newChunks) {
        chunks.addAll(newChunks);
    }

    public void removeByDoc(String docName) {
        chunks.removeIf(c -> c.docName().equals(docName));
    }

    public void rebuild(List<Chunk> newChunks) {
        chunks.clear();
        chunks.addAll(newChunks);
    }

    public List<Chunk> all() {
        return List.copyOf(chunks);
    }
}
