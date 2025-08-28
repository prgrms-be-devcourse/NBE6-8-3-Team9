package com.back.back9.domain.coin.entity

import com.back.back9.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "coin")
open class Coin @JvmOverloads constructor(
    @field:NotBlank
    @Column(unique = true, nullable = false)
    open var symbol: String,

    @Column(unique = true)
    open var koreanName: String?,

    @Column(unique = true)
    open var englishName: String?
) : BaseEntity() {

    // JPA가 필요로 하는 무인자 생성자 (plugin.jpa를 쓰더라도 안전하게 유지)
    constructor() : this("", null, null)

    // Lombok @Builder 호환 (Java에서 Coin.builder() 그대로 사용 가능)
    companion object {
        @JvmStatic fun builder() = Builder()
    }

    class Builder {
        private var symbol: String = ""
        private var koreanName: String? = null
        private var englishName: String? = null

        fun symbol(v: String) = apply { symbol = v }
        fun koreanName(v: String?) = apply { koreanName = v }
        fun englishName(v: String?) = apply { englishName = v }
        fun build() = Coin(symbol, koreanName, englishName)
    }
}