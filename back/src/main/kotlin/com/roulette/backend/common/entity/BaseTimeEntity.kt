package com.roulette.backend.common.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime
import java.time.ZoneOffset

@MappedSuperclass
abstract class BaseTimeEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
        protected set

    @PrePersist
    protected fun onPrePersist() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun onPreUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC)
    }
}
