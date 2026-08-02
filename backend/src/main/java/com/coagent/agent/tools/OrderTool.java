package com.coagent.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单查询工具（业务数据 Mock）。
 *
 * <p>方法标注 {@link Tool}，同一批工具亦可直接注册进
 * {@code ChatClient.defaultTools(...)} 交给模型自主调用（Function Calling）；
 * 当前演示中由编排层驱动调用，以保证可控性与可观测性。
 */
@Component
public class OrderTool {

    private static final Map<String, String> ORDERS = new LinkedHashMap<>();

    static {
        ORDERS.put("JD2025001", "订单 JD2025001：无线蓝牙耳机，状态【已签收】，签收时间 2025-06-20，金额 ¥299.00");
        ORDERS.put("JD2025002", "订单 JD2025002：智能手环 Pro，状态【运输中】，预计 2026-08-04 送达，承运方 顺丰速运");
        ORDERS.put("JD2025003", "订单 JD2025003：便携蓝牙音箱，状态【已发货】，承运方 圆通快递，运单号 YT7890123456");
        ORDERS.put("JD2025004", "订单 JD2025004：不锈钢保温杯，状态【待付款】，请尽快完成支付以锁定优惠");
        ORDERS.put("JD2025005", "订单 JD2025005：石墨烯电热毯，状态【派送中】，预计今日 20:00 前送达，请保持电话畅通");
    }

    @Tool(name = "查询订单状态", description = "根据订单号查询订单的当前状态、物流与金额信息")
    public String queryOrder(String orderNo) {
        if (orderNo == null || !ORDERS.containsKey(orderNo)) {
            return "未查询到订单 " + orderNo + "，请核对订单号（示例：JD2025001）。";
        }
        return ORDERS.get(orderNo);
    }
}
