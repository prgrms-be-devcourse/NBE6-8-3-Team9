package com.back.back9.domain.common.vo.money

import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Money private constructor(val amount: BigDecimal) {

    init {
        require(amount >= BigDecimal.ZERO) { "금액은 음수가 될 수 없습니다." }
    }

    fun toBigDecimal(): BigDecimal = amount

    fun add(other: Money): Money = Money(amount.add(other.amount))

    fun subtract(other: Money): Money = Money(amount.subtract(other.amount))

    fun multiply(multiplier: BigDecimal): Money = Money(amount.multiply(multiplier))

    fun divide(divisor: BigDecimal): Money =
        if (divisor == BigDecimal.ZERO) zero()
        else Money(amount.divide(divisor, SCALE, ROUNDING_MODE))

    val isPositive: Boolean get() = amount > BigDecimal.ZERO
    fun isGreaterThanZero(): Boolean = amount > BigDecimal.ZERO

    fun isGreaterThanOrEqual(other: Money): Boolean = amount >= other.amount

    val isZero: Boolean get() = amount.compareTo(BigDecimal.ZERO) == 0

    override fun toString(): String = amount.toPlainString()

    companion object {
        private const val SCALE = 8
        private val ROUNDING_MODE = RoundingMode.HALF_EVEN

        fun of(amount: BigDecimal): Money = Money(amount.setScale(SCALE, ROUNDING_MODE))

        fun of(value: Long): Money = Money(BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE))

        fun zero(): Money = Money(BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE))
    }
}
