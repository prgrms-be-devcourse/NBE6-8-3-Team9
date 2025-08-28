package com.back.back9.domain.coin.service

// 코인 정보에 변경이 발생했음을 알리는 이벤트 클래스.
class CoinDataChangedEvent(val source: Any) {
    override fun toString(): String {
        // 이벤트 발생지의 클래스 이름을 반환 (예: "CoinService")
        return "CoinDataChangedEvent(source=${source.javaClass.simpleName})"
    }
}
