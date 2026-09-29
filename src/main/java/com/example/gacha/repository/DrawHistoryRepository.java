package com.example.gacha.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gacha.domain.DrawHistory;

public interface DrawHistoryRepository extends JpaRepository<DrawHistory, Long> {

    List<DrawHistory> findAllByOrderByDrawnAtDesc();

    Optional<DrawHistory> findFirstByOrderByDrawnAtDesc();
}
