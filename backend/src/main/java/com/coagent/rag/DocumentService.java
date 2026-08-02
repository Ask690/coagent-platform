package com.coagent.rag;

import com.coagent.domain.KnowledgeDoc;
import com.coagent.rag.IndexStore.Chunk;
import com.coagent.repository.KnowledgeDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档服务：负责文档切块、入内存索引、入库持久化。
 * 重启后从 knowledge_doc 表重建索引。
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final KnowledgeDocRepository docRepository;
    private final IndexStore indexStore;
    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentService(KnowledgeDocRepository docRepository,
                           IndexStore indexStore,
                           @Value("${coagent.rag.chunk-size:280}") int chunkSize,
                           @Value("${coagent.rag.chunk-overlap:60}") int chunkOverlap) {
        this.docRepository = docRepository;
        this.indexStore = indexStore;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /** 导入一份文档：切块 -> 内存索引 -> 入库 */
    @Transactional
    public KnowledgeDoc ingest(String fileName, String content) {
        List<String> chunks = Chunker.split(content, chunkSize, chunkOverlap);
        indexStore.removeByDoc(fileName);
        List<Chunk> chunkList = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            chunkList.add(new Chunk(fileName + "#" + i, fileName, chunks.get(i)));
        }
        indexStore.addAll(chunkList);

        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setFileName(fileName);
        doc.setContent(content);
        doc.setChunkCount(chunks.size());
        docRepository.save(doc);
        log.info("知识库入库完成：{}，切块 {}", fileName, chunks.size());
        return doc;
    }

    public List<KnowledgeDoc> list() {
        return docRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void delete(Long id) {
        docRepository.findById(id).ifPresent(doc -> {
            indexStore.removeByDoc(doc.getFileName());
            docRepository.delete(doc);
        });
    }

    /** 首次启动导入内置种子知识文档（classpath:knowledge/*） */
    public void loadSeedsIfEmpty() throws IOException {
        if (docRepository.count() > 0) {
            return;
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:knowledge/*");
        for (Resource res : resources) {
            String fileName = res.getFilename();
            if (fileName == null || docRepository.existsByFileName(fileName)) {
                continue;
            }
            String content = res.getContentAsString(StandardCharsets.UTF_8);
            ingest(fileName, content);
        }
    }

    /** 从数据库全量重建内存索引（启动时调用） */
    public void rebuildIndexFromDb() {
        List<KnowledgeDoc> docs = docRepository.findAll();
        List<Chunk> all = new ArrayList<>();
        for (KnowledgeDoc doc : docs) {
            List<String> chunks = Chunker.split(doc.getContent(), chunkSize, chunkOverlap);
            for (int i = 0; i < chunks.size(); i++) {
                all.add(new Chunk(doc.getFileName() + "#" + i, doc.getFileName(), chunks.get(i)));
            }
        }
        indexStore.rebuild(all);
        log.info("知识库索引重建完成，共 {} 个知识块 / {} 篇文档", all.size(), docs.size());
    }
}
