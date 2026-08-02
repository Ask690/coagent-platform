package com.coagent.repository;

import com.coagent.domain.KnowledgeDoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocRepository extends JpaRepository<KnowledgeDoc, Long> {

    List<KnowledgeDoc> findAllByOrderByCreatedAtDesc();

    boolean existsByFileName(String fileName);
}
