package com.coagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CoAgent 企业智能客服 · 多智能体协作平台
 *
 * <p>技术栈：Spring Boot 3.5 + Spring AI 1.0 + DeepSeek + Vue3
 * <p>核心能力：多智能体编排（Supervisor 调度）、RAG 知识库检索、工具调用、SSE 流式对话
 */
@SpringBootApplication
public class CoAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoAgentApplication.class, args);
    }
}
