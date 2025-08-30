package com.back.back9.domain.common.vo.money

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.math.BigDecimal

@Converter(autoApply = true)
class MoneyConverter : AttributeConverter<Money, BigDecimal> {
    override fun convertToDatabaseColumn(attribute: Money?): BigDecimal? {
        if (attribute == null) {
            println("⚠️ convertToDatabaseColumn got NULL")
            return null
        }

        println("⚠️ convertToDatabaseColumn got ${attribute::class} = $attribute")

        return if (attribute is Money) {
            attribute.toBigDecimal()
        } else {
            // BigDecimal 같은 잘못된 타입이 들어왔을 때
            throw IllegalArgumentException("Expected Money but got ${attribute::class}: $attribute")
        }
    }

    override fun convertToEntityAttribute(dbData: BigDecimal?): Money? {
        if (dbData != null) {
            println("ℹ️ [MoneyConverter] convertToEntityAttribute got BigDecimal = $dbData")
        }
        return dbData?.let { Money.of(it) }
    }
}