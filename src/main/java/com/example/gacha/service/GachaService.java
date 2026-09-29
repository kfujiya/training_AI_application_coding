package com.example.gacha.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gacha.domain.DrawHistory;
import com.example.gacha.domain.GachaItem;
import com.example.gacha.repository.DrawHistoryRepository;
import com.example.gacha.repository.GachaItemRepository;

@Service
public class GachaService {

    private final GachaItemRepository gachaItemRepository;
    private final DrawHistoryRepository drawHistoryRepository;

    public GachaService(
            GachaItemRepository gachaItemRepository,
            DrawHistoryRepository drawHistoryRepository) {
        this.gachaItemRepository = gachaItemRepository;
        this.drawHistoryRepository = drawHistoryRepository;
    }

    @Transactional
    public DrawHistory draw() {
        List<GachaItem> candidates = gachaItemRepository.findByEnabledTrueAndWeightGreaterThan(0);
        if (candidates.isEmpty()) {
            throw new GachaUnavailableException("抽選対象のアイテムがありません。");
        }

        long totalWeight = candidates.stream()
                .mapToLong(item -> item.getWeight().longValue())
                .reduce(0L, GachaService::addWeight);
        long target = ThreadLocalRandom.current().nextLong(totalWeight);

        GachaItem selected = null;
        long cumulativeWeight = 0L;
        for (GachaItem candidate : candidates) {
            cumulativeWeight = addWeight(cumulativeWeight, candidate.getWeight().longValue());
            if (target < cumulativeWeight) {
                selected = candidate;
                break;
            }
        }

        if (selected == null) {
            throw new IllegalStateException("アイテムの抽選に失敗しました。");
        }

        DrawHistory history = new DrawHistory(selected, selected.getName(), selected.getRarity());
        return drawHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<DrawHistory> findHistories() {
        return drawHistoryRepository.findAllByOrderByDrawnAtDesc();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DrawHistory> findLatestHistory() {
        return drawHistoryRepository.findFirstByOrderByDrawnAtDesc();
    }

    private static long addWeight(long current, long weight) {
        if (Long.MAX_VALUE - current < weight) {
            throw new IllegalStateException("アイテムの重みの合計が大きすぎます。");
        }
        return current + weight;
    }
}
