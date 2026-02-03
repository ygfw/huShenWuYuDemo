package com.huyao.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.huyao.util.SnowflakeIdGenerator;

@Entity
@Table(name = "characters")
public class Character {
    @Id
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 255)
    private String image;

    @Column(columnDefinition = "text")
    private String ultimateDesc;

    @Column(columnDefinition = "text")
    private String skill1Desc;

    @Column(columnDefinition = "text")
    private String skill2Desc;

    @Column(columnDefinition = "text")
    private String quote;

    private LocalDateTime obtainedAt;

    @PrePersist
    public void assignId() {
        if (id == null) {
            id = SnowflakeIdGenerator.nextId();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUltimateDesc() {
        return ultimateDesc;
    }

    public void setUltimateDesc(String ultimateDesc) {
        this.ultimateDesc = ultimateDesc;
    }

    public String getSkill1Desc() {
        return skill1Desc;
    }

    public void setSkill1Desc(String skill1Desc) {
        this.skill1Desc = skill1Desc;
    }

    public String getSkill2Desc() {
        return skill2Desc;
    }

    public void setSkill2Desc(String skill2Desc) {
        this.skill2Desc = skill2Desc;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public LocalDateTime getObtainedAt() {
        return obtainedAt;
    }

    public void setObtainedAt(LocalDateTime obtainedAt) {
        this.obtainedAt = obtainedAt;
    }
}
