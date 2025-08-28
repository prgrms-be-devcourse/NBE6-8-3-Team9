package com.back.back9.domain.coin.entity;

import com.back.back9.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Coin extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    private String symbol;

    @Column(unique = true)
    private String koreanName;

    @Column(unique = true)
    private String englishName;

    public Coin() {
    }

    protected Coin(String symbol, String koreanName, String englishName) {
        this.symbol = symbol;
        this.koreanName = koreanName;
        this.englishName = englishName;
    }

    public static CoinBuilder builder() {
        return new CoinBuilder();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public void setKoreanName(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public static class CoinBuilder {
        private String symbol;
        private String koreanName;
        private String englishName;

        CoinBuilder() {
        }

        public CoinBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public CoinBuilder koreanName(String koreanName) {
            this.koreanName = koreanName;
            return this;
        }

        public CoinBuilder englishName(String englishName) {
            this.englishName = englishName;
            return this;
        }

        public Coin build() {
            return new Coin(symbol, koreanName, englishName);
        }

        public String toString() {
            return "Coin.CoinBuilder(symbol=" + this.symbol + ", koreanName=" + this.koreanName + ", englishName=" + this.englishName + ")";
        }
    }
}
