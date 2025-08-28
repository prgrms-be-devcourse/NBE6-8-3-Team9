package com.back.back9.domain.coin.controller

import com.back.back9.domain.coin.dto.CoinAddRequest
import com.back.back9.domain.coin.dto.CoinDto
import com.back.back9.domain.coin.service.CoinService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/adm/coins")
@CrossOrigin(origins = ["http://localhost:3000"])
class CoinController(
    private val coinService: CoinService
) {
    // 전체 코인 조회 (GET)
    @GetMapping
    fun getCoins(): ResponseEntity<List<CoinDto>> {
        val coins = coinService.findAll()
            .map { CoinDto(it) }
        return ResponseEntity.ok(coins)
    }

    // 코인 단건 조회 (GET)
    @GetMapping("/{id}")
    @Transactional
    fun getCoin(@PathVariable id: Long): ResponseEntity<CoinDto> {
        val coin = coinService.findById(id)
        return ResponseEntity.ok(CoinDto(coin))
    }

    // 코인 삭제 (DELETE)
    @DeleteMapping("/{id}")
    @Transactional
    fun deleteCoin(@PathVariable id: Long): ResponseEntity<String> {
        val coin = coinService.findById(id)
        coinService.delete(coin)
        val result = "$id 번 코인이 삭제 되었습니다."
        return ResponseEntity.ok(result)
    }


    // 코인 추가 (POST)
    @PostMapping
    @Transactional
    fun addCoin(@RequestBody reqBody: @Valid CoinAddRequest): ResponseEntity<CoinDto> {
        val coin = coinService.add(reqBody.symbol, reqBody.koreanName, reqBody.englishName)

        return ResponseEntity.ok(CoinDto(coin))
    }

    // 코인 수정 (PUT)
    @PutMapping("/{id}")
    @Transactional
    fun modifyCoin(
        @PathVariable id: Long,
        @RequestBody reqBody: CoinAddRequest
    ): ResponseEntity<String> {
        val coin = coinService!!.findById(id)

        coinService.modify(coin, reqBody.symbol, reqBody.koreanName, reqBody.englishName)

        val result = "$id 번 코인이 수정되었습니다."

        return ResponseEntity.ok(result)
    }
}
