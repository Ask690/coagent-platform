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
    private volatile long version = 0;

    public void addAll(List<Chunk> newChunks) {
        chunks.addAll(newChunks);
        version++;
    }

    public void removeByDoc(String docName) {
        chunks.removeIf(c -> c.docName().equals(docName));
        version++;
    }

    public void rebuild(List<Chunk> newChunks) {
        chunks.clear();
        chunks.addAll(newChunks);
        version++;
    }

    public List<Chunk> all() {
        return List.copyOf(chunks);
    }

    /** 内容版本号：变更时自增，供检索器判断是否需要重建索引/向量缓存 */
    public long version() {
        return version;
    }
}
