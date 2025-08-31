package com.back.back9.domain.exchange.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "exchange",
    indexes = [Index(name = "idx_symbol_candletime", columnList = "symbol, candleTime")]
)
data class Exchange(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var symbol: String,

    @Column(nullable = false)
    var candleTime: LocalDateTime,

    @Column(precision = 18, scale = 8, nullable = false)
    var open: BigDecimal,

    @Column(precision = 18, scale = 8, nullable = false)
    var high: BigDecimal,

    @Column(precision = 18, scale = 8, nullable = false)
    var low: BigDecimal,

    @Column(precision = 18, scale = 8, nullable = false)
    var close: BigDecimal,

    @Column(precision = 18, scale = 8, nullable = false)
    var volume: BigDecimal,

    @Column(nullable = false)
    var timestamp: Long
)