package com.coagent.controller;

import com.coagent.domain.Ticket;
import com.coagent.repository.TicketRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 工单中心：列表 / 详情 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;

    public TicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @GetMapping
    public List<Ticket> list() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{ticketNo}")
    public ResponseEntity<Ticket> detail(@PathVariable String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
