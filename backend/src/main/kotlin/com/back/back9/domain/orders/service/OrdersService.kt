package com.back.back9.domain.orders.service

import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.orders.dto.OrderResponse
import com.back.back9.domain.orders.dto.OrdersRequest
import com.back.back9.domain.orders.entity.Orders
import com.back.back9.domain.orders.entity.OrdersStatus
import com.back.back9.domain.orders.repository.OrdersRepository
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.wallet.dto.BuyCoinRequest
import com.back.back9.domain.wallet.dto.TransactionType
import com.back.back9.domain.wallet.dto.WalletResponse
import com.back.back9.domain.wallet.repository.WalletRepository
import com.back.back9.domain.wallet.service.WalletService
import com.back.back9.global.error.ErrorException
import lombok.extern.slf4j.Slf4j
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.function.Supplier
import java.util.stream.Collectors


@Service
@Slf4j
class OrdersService(
    private val ordersRepository: OrdersRepository,
    private val coinRepository: CoinRepository,
    private val walletRepository: WalletRepository,
    private val walletService: WalletService
) {
    @Transactional
    fun getOrdersByWalletId(walletId: Long?): List<OrderResponse?> {
        val orders = ordersRepository.findByWalletId(walletId)

        return orders.map { order -> OrderResponse.from(order) }
    }

    @Transactional
    fun executeTrade(walletId: Long, ordersRequest: OrdersRequest): OrderResponse {
//        OrdersService.log.info(ordersRequest.toString())
        val wallet = walletRepository.findById(walletId)
            .orElseThrow<IllegalArgumentException?>(Supplier { IllegalArgumentException("지갑을 찾을 수 없습니다.") })
        val user = wallet.user

        val coin = coinRepository.findBySymbol(ordersRequest.coinSymbol)
            .orElseThrow<IllegalArgumentException?>(Supplier { IllegalArgumentException("코인을 찾을 수 없습니다.") })

        //1. BuyCoinRequest생성
        val buyCoinRequest = BuyCoinRequest(
            coin.getId(),
            wallet.getId(),
            ordersRequest.price.multiply(ordersRequest.quantity),
            ordersRequest.quantity
        )
        //2. walletService.processTransaction 메서드 호출 및 결과 값 확인
        val transactionType = if (ordersRequest.tradeType == TradeType.BUY)
            TransactionType.BUY
        else
            TransactionType.SELL

        //3. 성공 여부에 따라 Order 엔티티 저장
        try {
            val walletResponse: ResponseEntity<WalletResponse> =
                walletService.processTransaction(user.getId(), buyCoinRequest, transactionType)

            if (walletResponse.getStatusCode().is2xxSuccessful()) {
//                OrdersService.log.info("거래 성공: {}", walletResponse.getBody())

                val savedOrder = Orders.builder()
                    .user(user)
                    .wallet(wallet)
                    .coin(coin)
                    .tradeType(ordersRequest.tradeType)
                    .ordersMethod(ordersRequest.ordersMethod)
                    .quantity(ordersRequest.quantity)
                    .price(ordersRequest.price)
                    .createdAt(LocalDateTime.now())
                    .ordersStatus(OrdersStatus.FILLED)
                    .build()

                ordersRepository.save<Orders?>(savedOrder)
                return OrderResponse.from(savedOrder)
            }

            // 성공 아니지만 예외도 안 난 경우
//            OrdersService.log.error("거래 실패: {}", walletResponse.getStatusCode())
        } catch (e: ErrorException) {
            // 실패한 이유를 로그에 남기고, FAILED 상태로 주문 저장
//            OrdersService.log.error("거래 예외 발생 - 실패 사유: {}", e.message)

            val failedOrder = Orders.builder()
                .user(user)
                .wallet(wallet)
                .coin(coin)
                .tradeType(ordersRequest.tradeType)
                .ordersMethod(ordersRequest.ordersMethod)
                .quantity(ordersRequest.quantity)
                .price(ordersRequest.price)
                .createdAt(LocalDateTime.now())
                .ordersStatus(OrdersStatus.FAILED)
                .build()

            ordersRepository.save<Orders?>(failedOrder)
            return OrderResponse.from(failedOrder)
        }

        throw IllegalArgumentException("예상치 못한 거래 실패")
    }
}