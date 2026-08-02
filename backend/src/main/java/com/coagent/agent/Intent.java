package com.coagent.agent;

/** 用户意图：编排者据此把请求分派给对应的专精 Agent */
public enum Intent {
    /** 知识库问答（RAG 检索） */
    KNOWLEDGE,
    /** 业务数据查询（订单 / 物流，走工具调用） */
    BUSINESS,
    /** 投诉 / 售后需人工处理（创建工单） */
    TICKET,
    /** 寒暄问候等，直接回复 */
    DIRECT,
    /** 组合场景：需多步链路（先查订单数据，再检索政策，最后汇总） */
    CHAIN
}
