package com.coagent.agent;

/**
 * 意图路由策略。
 *
 * <p>两种实现，通过 coagent.ai.mock 切换：
 * <ul>
 *   <li>mock=true  —— 关键词规则路由（KeywordRouter），离线兜底，永远可用</li>
 *   <li>mock=false —— 大模型结构化路由（LlmRouter），利用 DeepSeek 判断意图</li>
 * </ul>
 */
public interface Router {

    RoutingDecision route(String userMessage);
}
