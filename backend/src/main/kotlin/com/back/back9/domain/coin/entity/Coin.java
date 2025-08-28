package com.back.back9.domain.coin.entity;

import com.back.back9.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Coin extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    private String symbol;

    @Column(unique = true)
    private String koreanName;

    @Column(unique = true)
    private String englishName;


}
