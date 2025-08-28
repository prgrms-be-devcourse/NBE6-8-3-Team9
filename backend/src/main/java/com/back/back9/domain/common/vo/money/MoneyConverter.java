package com.back.back9.domain.common.vo.money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Money money) {
        return money != null ? money.toBigDecimal() : null;
    }

    @Override
    public Money convertToEntityAttribute(BigDecimal dbData) {
        return dbData != null ? Money.of(dbData) : null;
    }
}
