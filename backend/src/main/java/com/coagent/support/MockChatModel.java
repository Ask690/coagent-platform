package com.coagent.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置 Mock 大模型：零 API Key 时也能完整演示多智能体协作。
 *
 * <p>实现 Spring AI 标准的 {@link ChatModel} 接口，接入的是同一套
 * ChatClient 流式管线 —— 只需把 coagent.ai.mock 置为 false 并配置
 * DeepSeek API Key，即无缝切换到真实模型，代码零改动。
 *
 * <p>Mock 响应会读取提示词中约定好的上下文标记：
 * 【知识库上下文】…【/知识库上下文】、【工具结果】…【/工具结果】
 */
@Component
@Primary
@ConditionalOnProperty(name = "coagent.ai.mock", havingValue = "true", matchIfMissing = true)
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(generate(prompt)))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        String text = generate(prompt);
        return Flux.fromIterable(splitChunks(text, 6))
                .map(chunk -> new ChatResponse(List.of(new Generation(new AssistantMessage(chunk)))));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }

    // ================= 以下为 Mock 文本生成逻辑 =================

    private String generate(Prompt prompt) {
        String userText = lastUserText(prompt);
        if (userText == null) {
            userText = "";
        }
        // 当前问题 = 用户消息正文（第一个上下文标记之前的部分），避免上下文内容干扰意图判断
        String current = userText;
        int markerIdx = userText.indexOf("【");
        if (markerIdx > 0) {
            current = userText.substring(0, markerIdx);
        }
        String knowledge = extractBlock(userText, "【知识库上下文】", "【/知识库上下文】");
        String toolResult = extractBlock(userText, "【工具结果】", "【/工具结果】");
        String snippet = firstSentence(knowledge.isEmpty() ? toolResult : knowledge);

        // ===== 链路汇总场景：汇总Agent 同时收到订单数据与政策资料 =====
        String orderBlock = extractBlock(userText, "【订单数据】", "【/订单数据】");
        String policyBlock = extractBlock(userText, "【政策资料】", "【/政策资料】");
        if (!orderBlock.isEmpty() && !policyBlock.isEmpty()) {
            return buildChainSummary(orderBlock, policyBlock);
        }

        if (toolResult.contains("工单") && (current.contains("工单") || current.contains("投诉"))) {
            return "好的，已为您自动创建售后工单。" + wrap("工单编号 " + firstSentence(toolResult) + " 已生成，客服团队将在 1 个工作日内跟进处理。") +
                    "您也可以在左侧「工单中心」查看进度。请问还有其他可以帮您的吗？";
        }
        if (current.contains("订单") || current.contains("物流") || current.contains("发货")
                || current.contains("快递") || current.contains("到哪") || current.contains("配送")) {
            if (toolResult.isEmpty()) {
                return "可以为您查询订单与物流信息。请提供您的订单号（格式如 JD2025001），我马上帮您查询。";
            }
            return "已为您查询到订单信息：" + wrap(snippet) + "如需更详细的物流节点，请提供完整订单号。";
        }
        if (current.contains("退货") || current.contains("退款") || current.contains("换货")
                || current.contains("售后") || current.contains("发票") || current.contains("保修")) {
            return "为您查询到相关政策：" + wrap(snippet) + "如有特殊情况（如已超退货期），建议转人工工单处理，我可以帮您创建。";
        }
        if (current.contains("会员") || current.contains("积分") || current.contains("优惠")) {
            return "关于会员与积分规则：" + wrap(snippet) + "您的会员等级可在个人中心查看。";
        }
        if (current.contains("你好") || current.contains("您好") || current.contains("hello")
                || current.contains("hi") || current.contains("在吗")) {
            return "您好！我是优购商城智能客服「小优」，由多个 AI Agent 协作为您服务，可以处理订单物流查询、退换货政策、发票开具、工单投诉等问题，请问有什么可以帮您？";
        }
        if (!knowledge.isEmpty()) {
            return "根据知识库检索，为您找到相关信息：" + wrap(snippet) + "如需更详细内容，可以继续追问。";
        }
        return "收到您的问题，我已经记录了。您可以试着问我「订单物流」「退货政策」「开发票」「投诉」等，我会调用对应的 Agent 与工具为您处理。";
    }

    /** 链路汇总：综合订单状态与退换政策，给出结构化结论 */
    private String buildChainSummary(String orderBlock, String policyBlock) {
        String orderSentence = firstSentence(orderBlock);
        String policySentence = firstSentence(policyBlock);
        StringBuilder sb = new StringBuilder("已为您综合查询订单与相关政策，结论如下：\n\n");
        sb.append("【订单状态】").append(orderSentence).append("\n\n");
        sb.append("【适用政策】").append(policySentence).append("\n\n");
        sb.append("【结论与建议】\n");
        if (orderBlock.contains("已签收")) {
            sb.append("您的订单已签收，仍在七（7）天无理由退货期内的话，可直接发起退货申请；超过期限则建议提供质量凭证申请换货或创建人工工单。");
        } else if (orderBlock.contains("运输中") || orderBlock.contains("已发货") || orderBlock.contains("派送中")) {
            sb.append("您的订单尚未签收，暂不符合退货条件，建议先签收验货；如商品到货后有问题，可在签收后按政策申请退换。");
        } else if (orderBlock.contains("未查询到") || orderBlock.contains("未找到")) {
            sb.append("请先核对订单号（格式如 JD2025001），确认后可为您查询具体退换条件。");
        } else {
            sb.append("请核对订单当前状态后，在订单详情页发起退货/换货申请，或联系在线客服协助。");
        }
        return sb.toString();
    }

    private String wrap(String s) {
        return s.isBlank() ? "" : "根据查询结果：" + s + "。";
    }

    private String lastUserText(Prompt prompt) {
        String last = null;
        for (Message m : prompt.getInstructions()) {
            if (m.getMessageType() == MessageType.USER) {
                last = m.getText();
            }
        }
        return last;
    }

    /** 提取标记块内容（去掉标记行） */
    private String extractBlock(String text, String startMark, String endMark) {
        int s = text.indexOf(startMark);
        if (s < 0) {
            return "";
        }
        s += startMark.length();
        int e = text.indexOf(endMark, s);
        if (e < 0) {
            return text.substring(s).trim();
        }
        return text.substring(s, e).trim();
    }

    private String firstSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.trim().replaceAll("\\s+", " ");
        int end = -1;
        for (String sep : new String[]{"。", "！", "？", "！？", ".\n", "!"}) {
            int idx = t.indexOf(sep);
            if (idx > 0 && (end < 0 || idx < end)) {
                end = idx;
            }
        }
        if (end > 0 && t.length() > 15) {
            return t.substring(0, end + 1);
        }
        return t.length() > 60 ? t.substring(0, 60) + "…" : t;
    }

    private List<String> splitChunks(String text, int size) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < text.length(); i += size) {
            out.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return out;
    }
}
