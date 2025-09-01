package com.back.back9.domain.orders.orders.service

import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.orders.entity.OrdersStatus
import com.back.back9.domain.orders.orders.repository.OrdersRepository
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.wallet.dto.BuyCoinRequest
import com.back.back9.domain.wallet.dto.TransactionType
import com.back.back9.domain.wallet.repository.WalletRepository
import com.back.back9.domain.wallet.service.WalletService
import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.function.Supplier


@Service
@Slf4j
class OrdersService(
    private val ordersRepository: OrdersRepository,
    private val coinRepository: CoinRepository,
    private val walletRepository: WalletRepository,
    private val walletService: WalletService,
) {
    @Transactional
    fun getOrdersByWalletId(walletId: Long?): List<OrderResponse?> {
        val orders = ordersRepository.findByWalletId(walletId)

        return orders.map { order -> OrderResponse.from(order) }
    }

    @Transactional
    fun executeMarketOrder(walletId: Long, ordersRequest: OrdersRequest): OrderResponse {
//        OrdersService.log.info(ordersRequest.toString())
        val wallet = walletRepository.findById(walletId)
            .orElseThrow<IllegalArgumentException?>(Supplier { IllegalArgumentException("지갑을 찾을 수 없습니다.") })
        val user = wallet.user

        val coin = coinRepository.findBySymbol(ordersRequest.coinSymbol)
            .orElseThrow<IllegalArgumentException?>(Supplier { IllegalArgumentException("코인을 찾을 수 없습니다.") })

        // 기존 즉시 체결 로직
        val buyCoinRequest = coin.id?.let {
            wallet.id?.let { it1 ->
                BuyCoinRequest(
                    it,
                    it1,
                    ordersRequest.price.multiply(ordersRequest.quantity),
                    ordersRequest.quantity
                )
            }
        }

        val transactionType = if (ordersRequest.tradeType == TradeType.BUY)
            TransactionType.BUY else TransactionType.SELL

        val walletResponse = user.id?.let { buyCoinRequest?.let { request -> walletService.processTransaction(it, request, transactionType) } }

        if (walletResponse != null) {
            if (walletResponse.statusCode.is2xxSuccessful) {
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

                ordersRepository.save(savedOrder)
                return OrderResponse.from(savedOrder)
            } else {
                throw IllegalArgumentException("거래 실패: ${walletResponse.statusCode}")
            }
        }
        throw IllegalStateException("거래 처리 중 알 수 없는 오류가 발생했습니다. walletResponse=null")

    }
}