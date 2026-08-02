package com.coagent.agent.tools;

import com.coagent.domain.Ticket;
import com.coagent.repository.TicketRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 工单工具：创建 / 查询 / 关闭工单，真实落库到 ticket 表。
 */
@Component
public class TicketTool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketRepository ticketRepository;

    public TicketTool(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Tool(name = "创建工单", description = "创建售后/投诉/保修工单并返回工单编号")
    public String createTicket(String type, String orderNo, String content) {
        Ticket ticket = new Ticket();
        // 先用随机占位号入库拿到自增 id，再以「日期-id」生成对外工单号，
        // 保证服务重启、多实例部署下都不会撞唯一索引
        ticket.setTicketNo("TMP-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            ticket.setType(Ticket.Type.valueOf(type.toUpperCase()));
        } catch (Exception e) {
            ticket.setType(Ticket.Type.CONSULT);
        }
        ticket.setOrderNo(isBlank(orderNo) ? null : orderNo);
        String title = content == null ? "用户咨询" : content.replaceAll("\\s+", " ");
        ticket.setTitle(title.length() > 50 ? title.substring(0, 50) : title);
        ticket.setContent(content);
        ticketRepository.save(ticket);

        ticket.setTicketNo("TK" + LocalDateTime.now().format(FMT) + "-" + ticket.getId());
        ticketRepository.save(ticket);
        return ticket.getTicketNo();
    }

    @Tool(name = "查询工单进度", description = "按工单号查询工单类型与处理状态")
    public String queryTicket(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .map(t -> "工单 " + ticketNo + "：类型 " + t.getType() + "，状态 " + t.getStatus()
                        + "，创建于 " + t.getCreatedAt())
                .orElse("未找到工单 " + ticketNo);
    }

    @Tool(name = "关闭工单", description = "关闭已解决或用户确认完成的工单")
    public String closeTicket(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .map(t -> {
                    t.setStatus(Ticket.Status.CLOSED);
                    t.setUpdatedAt(LocalDateTime.now());
                    ticketRepository.save(t);
                    return "工单 " + ticketNo + " 已关闭。";
                })
                .orElse("未找到工单 " + ticketNo);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
