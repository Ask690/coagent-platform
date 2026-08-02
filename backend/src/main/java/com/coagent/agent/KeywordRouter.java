package com.coagent.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 关键词规则路由：零 API Key 也能跑通，作为大模型路由的降级兜底。
 * 规则可配置化，适合冷启动与应急场景。
 */
@Component
@ConditionalOnProperty(name = "coagent.ai.mock", havingValue = "true", matchIfMissing = true)
public class KeywordRouter implements Router {

    @Override
    public RoutingDecision route(String userMessage) {
        String msg = userMessage == null ? "" : userMessage;

        // 工单 / 投诉 / 转人工
        if (containsAny(msg, "工单", "投诉", "人工", "转人工", "上报", "申诉", "举报")) {
            return RoutingDecision.of(Intent.TICKET, "用户表达不满或需人工介入，创建工单");
        }
        // 业务数据：订单 / 物流
        if (containsAny(msg, "订单", "物流", "发货", "快递", "到哪", "配送", "签收", "单号", "JD")) {
            return RoutingDecision.of(Intent.BUSINESS, "需要查询订单或物流等业务数据");
        }
        // 知识库：政策 / 规则
        if (containsAny(msg, "退货", "退款", "换货", "发票", "保修", "政策", "规则", "多久", "时效",
                "会员", "积分", "优惠", "怎么", "如何", "为什么", "支持")) {
            return RoutingDecision.of(Intent.KNOWLEDGE, "属于政策或知识类问题，检索知识库回答");
        }
        // 寒暄
        if (containsAny(msg, "你好", "您好", "hello", "hi", "在吗", "嗨", "谢谢", "感谢")) {
            return RoutingDecision.of(Intent.DIRECT, "寒暄问候，直接友好回复");
        }
        return RoutingDecision.of(Intent.KNOWLEDGE, "默认走知识库检索兜底");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
