package com.coagent.controller;

import com.coagent.domain.KnowledgeDoc;
import com.coagent.rag.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 知识库文档管理：上传（自动切块入库）/ 列表 / 删除 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final List<String> ALLOWED_SUFFIXES = List.of(".txt", ".md", ".markdown");

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
        }
        String fileName = file.getOriginalFilename() == null ? "未命名.txt" : file.getOriginalFilename();
        String lower = fileName.toLowerCase(Locale.ROOT);
        boolean allowed = ALLOWED_SUFFIXES.stream().anyMatch(lower::endsWith);
        if (!allowed) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持 .txt / .md 文本文件"));
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件内容为空"));
            }
            KnowledgeDoc doc = documentService.ingest(fileName, content);
            return ResponseEntity.ok(doc);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<KnowledgeDoc> list() {
        return documentService.list();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
