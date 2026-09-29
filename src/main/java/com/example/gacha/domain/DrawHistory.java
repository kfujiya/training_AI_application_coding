package com.example.gacha.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "draw_histories")
public class DrawHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private GachaItem item;

    @Column(name = "item_name_snapshot", nullable = false, length = 255)
    private String itemNameSnapshot;

    @Column(name = "rarity_snapshot", nullable = false, length = 50)
    private String raritySnapshot;

    @Column(name = "drawn_at", nullable = false)
    private LocalDateTime drawnAt;

    protected DrawHistory() {
    }

    public DrawHistory(GachaItem item, String itemNameSnapshot, String raritySnapshot) {
        this.item = item;
        this.itemNameSnapshot = itemNameSnapshot;
        this.raritySnapshot = raritySnapshot;
    }

    @PrePersist
    void setDrawnAtIfMissing() {
        if (drawnAt == null) {
            drawnAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public GachaItem getItem() {
        return item;
    }

    public String getItemNameSnapshot() {
        return itemNameSnapshot;
    }

    public String getRaritySnapshot() {
        return raritySnapshot;
    }

    public LocalDateTime getDrawnAt() {
        return drawnAt;
    }
}
