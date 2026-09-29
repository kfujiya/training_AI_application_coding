package com.example.gacha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gacha_items")
public class GachaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String rarity;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private Boolean enabled = true;

    protected GachaItem() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRarity() {
        return rarity;
    }

    public Integer getWeight() {
        return weight;
    }

    public Boolean getEnabled() {
        return enabled;
    }
}
