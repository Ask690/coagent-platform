package com.coagent.agent;

import java.util.Map;

/**
 * SSE 流式事件：后端 → 前端，前端据此渲染 Agent 活动时间线与流式回复。
 *
 * <p>事件类型：
 * <ul>
 *   <li>session    —— 会话号（首次消息返回）</li>
 *   <li>agent_start —— Agent 开始工作（name / title）</li>
 *   <li>route      —— 编排者路由决策（intent / reason）</li>
 *   <li>tool_call  —— 工具调用（name / args）</li>
 *   <li>tool_result—— 工具结果（name / result）</li>
 *   <li>token      —— 流式文本增量（text）</li>
 *   <li>agent_end  —— Agent 结束</li>
 *   <li>done       —— 本轮回复结束</li>
 *   <li>error      —— 异常信息</li>
 * </ul>
 */
public record ChatEvent(String type, Map<String, Object> data) {

    public static ChatEvent of(String type, Map<String, Object> data) {
        return new ChatEvent(type, data);
    }

    public static ChatEvent kv(String type, String k1, Object v1) {
        return new ChatEvent(type, Map.of(k1, v1));
    }

    public static ChatEvent kv(String type, String k1, Object v1, String k2, Object v2) {
        return new ChatEvent(type, Map.of(k1, v1, k2, v2));
    }

    public static ChatEvent session(String sessionId) {
        return kv("session", "sessionId", sessionId);
    }

    public static ChatEvent agentStart(String name, String title) {
        return new ChatEvent("agent_start", Map.of("name", name, "title", title));
    }

    public static ChatEvent agentEnd(String name) {
        return kv("agent_end", "name", name);
    }

    public static ChatEvent route(Intent intent, String reason) {
        return new ChatEvent("route", Map.of("intent", intent.name(), "reason", reason));
    }

    public static ChatEvent token(String text) {
        return kv("token", "text", text);
    }

    public static ChatEvent toolCall(String name, Map<String, Object> args) {
        return new ChatEvent("tool_call", Map.of("name", name, "args", args));
    }

    public static ChatEvent toolResult(String name, Object result) {
        return new ChatEvent("tool_result", Map.of("name", name, "result", result));
    }

    public static ChatEvent done(String sessionId) {
        return kv("done", "sessionId", sessionId);
    }

    public static ChatEvent error(String message) {
        return kv("error", "message", message);
    }
}
