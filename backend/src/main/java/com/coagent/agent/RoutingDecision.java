package com.coagent.agent;

/** 编排者（Supervisor）的路由决策 */
public record RoutingDecision(Intent intent, String reason, String directAnswer) {

    public static RoutingDecision of(Intent intent, String reason) {
        return new RoutingDecision(intent, reason, null);
    }
}
