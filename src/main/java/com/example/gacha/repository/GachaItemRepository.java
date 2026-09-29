package com.example.gacha.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gacha.domain.GachaItem;

public interface GachaItemRepository extends JpaRepository<GachaItem, Long> {

    List<GachaItem> findByEnabledTrueAndWeightGreaterThan(int weight);
}
