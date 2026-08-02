package com.coagent.dto;

import jakarta.validation.constraints.NotBlank;

/** 对话请求体 */
public record ChatRequest(
        String sessionId,
        @NotBlank(message = "消息不能为空") String message) {
}
