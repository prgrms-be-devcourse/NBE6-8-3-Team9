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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Service
class OrdersService(
    private val ordersRepository: OrdersRepository,
    private val coinRepository: CoinRepository,
    private val walletRepository: WalletRepository,
    private val walletService: WalletService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    @Transactional
    fun getOrdersByWalletId(walletId: Long?): List<OrderResponse?> {
        val orders = ordersRepository.findByWalletId(walletId)

        return orders.map { order -> OrderResponse.from(order) }
    }

    @Transactional
    fun executeOrder(walletId: Long, ordersRequest: OrdersRequest): Orders {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { IllegalArgumentException("지갑을 찾을 수 없습니다.") }
        val user = wallet.user

        val coin = coinRepository.findBySymbol(ordersRequest.coinSymbol)
            .orElseThrow { IllegalArgumentException("코인을 찾을 수 없습니다.") }

        val order = Orders.builder()
            .user(user)
            .wallet(wallet)
            .coin(coin)
            .tradeType(ordersRequest.tradeType)
            .ordersMethod(ordersRequest.ordersMethod)
            .quantity(ordersRequest.quantity)
            .price(ordersRequest.price)
            .ordersStatus(OrdersStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build()

        val walletResponse = user.id?.let { userId ->
            val req = BuyCoinRequest(
                coin.id!!,
                wallet.id!!,
                ordersRequest.price.multiply(ordersRequest.quantity),
                ordersRequest.quantity
            )
            walletService.processTransaction(
                userId,
                req,
                if (ordersRequest.tradeType == TradeType.BUY) TransactionType.BUY else TransactionType.SELL
            )
        }

        return if (walletResponse != null && walletResponse.statusCode.is2xxSuccessful) {
            log.info("✅ 주문 체결 성공: walletResponse={}, orderId={}, status={}",
                walletResponse.statusCode, order.id, order.ordersStatus)

            order.markFilled()
            ordersRepository.save(order)
        } else {
            log.warn("❌ 주문 체결 실패: walletResponse={}, orderId={}, status={}",
                walletResponse?.statusCode, order.id, order.ordersStatus)
            order.markFailed("거래 실패: ${walletResponse?.statusCode ?: "알 수 없음"}")
            ordersRepository.save(order)
            throw IllegalArgumentException("거래 실패: ${walletResponse?.statusCode ?: "알 수 없음"}")
        }
    }

    @Transactional
    fun cancelOrder(orderId: Long): OrderResponse {
        val order = ordersRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("주문을 찾을 수 없습니다.") }

        // 상태 검사 (이미 체결된 주문은 취소 불가)
        if (order!!.ordersStatus != OrdersStatus.PENDING) {
            throw IllegalStateException("대기 중인 주문만 취소 할 수 있습니다.")
        }

        order.markCancelled()  // 엔티티 상태 전환
        return OrderResponse.from(order)
    }

    @Transactional
    fun createPendingOrder(walletId: Long, request: OrdersRequest): OrderResponse  {
        val wallet = walletRepository.findById(walletId).orElseThrow()
        val coin = coinRepository.findBySymbol(request.coinSymbol).orElseThrow()

        val order = Orders(
            user = wallet.user,
            wallet = wallet,
            coin = coin,
            tradeType = request.tradeType,
            ordersMethod = request.ordersMethod,
            quantity = request.quantity,
            price = request.price,
            ordersStatus = OrdersStatus.PENDING
        )
        return OrderResponse.from(ordersRepository.save(order))
    }
}