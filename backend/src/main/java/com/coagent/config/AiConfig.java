package com.coagent.config;

import com.coagent.rag.DocumentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关装配：
 * <ul>
 *   <li>共享 ChatClient（各 Agent 每次调用覆盖系统提示词，互不干扰）</li>
 *   <li>启动时重建知识库索引 + 首启导入种子文档</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /** 启动初始化：先重建索引（服务重启后数据仍在），再在空库时导入种子文档 */
    @Bean
    ApplicationRunner knowledgeInitRunner(DocumentService documentService) {
        return args -> {
            documentService.rebuildIndexFromDb();
            documentService.loadSeedsIfEmpty();
        };
    }
}
