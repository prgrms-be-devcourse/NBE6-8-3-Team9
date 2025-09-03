package com.back.back9.domain.orders.orders.service

import com.back.back9.domain.coin.entity.Coin
import com.back.back9.domain.coin.repository.CoinRepository
import com.back.back9.domain.common.vo.money.Money
import com.back.back9.domain.orders.orders.dto.OrderResponse
import com.back.back9.domain.orders.orders.dto.OrdersRequest
import com.back.back9.domain.orders.orders.entity.Orders
import com.back.back9.domain.orders.orders.entity.OrdersMethod
import com.back.back9.domain.orders.orders.entity.OrdersStatus
import com.back.back9.domain.orders.orders.repository.OrdersRepository
import com.back.back9.domain.tradeLog.entity.TradeLog
import com.back.back9.domain.tradeLog.entity.TradeType
import com.back.back9.domain.wallet.dto.BuyCoinRequest
import com.back.back9.domain.wallet.dto.TransactionType
import com.back.back9.domain.wallet.repository.WalletRepository
import com.back.back9.domain.wallet.service.WalletService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

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
    open fun getOrdersByFilter(
        walletId: Long,
        coinSymbol: String?,
        tradeType: TradeType?,
        orderMethod: OrdersMethod?,
        orderStatus: OrdersStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        pageable: Pageable
    ): Page<OrderResponse?> {
        val orders: Page<Orders> = ordersRepository.findByOrdersFilter(
            walletId = walletId,
            coinSymbol = coinSymbol,
            tradeType = tradeType,
            orderMethod = orderMethod,
            orderStatus = orderStatus,
            startDate = startDate,
            endDate = endDate,
            pageable = pageable
        )

        return orders.map { order -> OrderResponse.from(order) }
    }
    /**
     * 신규 주문 생성 (기본 상태: PENDING)
     */
    @Transactional
    fun createOrder(walletId: Long, ordersRequest: OrdersRequest): Orders {
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
            .ordersStatus(OrdersStatus.PENDING)  // ✅ 항상 PENDING으로 시작
            .createdAt(LocalDateTime.now())
            .build()

        return ordersRepository.save(order)
    }
    /**
     * 주문 상태 변경 (예: FILLED, FAILED, CANCELLED 등)
     */
    @Transactional
    fun updateOrderStatus(orderId: Long, newStatus: OrdersStatus, reason: String? = null): Orders {
        val order = ordersRepository.findById(orderId)
            .orElseThrow { IllegalArgumentException("주문을 찾을 수 없습니다. ID=$orderId") }

        when (newStatus) {
            OrdersStatus.FILLED -> order.markFilled()
            OrdersStatus.CANCELLED -> order.markCancelled()
            OrdersStatus.FAILED -> order.markFailed(reason ?: "실패 사유 없음")
            OrdersStatus.EXPIRED -> order.markExpired()
            OrdersStatus.PARTIALLY_FILLED -> order.markPartiallyFilled()
            else -> log.warn("Unsupported status update: $newStatus")
        }

        return ordersRepository.save(order)
    }

    /**
     * 주문 실행 로직 (예전 executeOrder)
     * → 내부적으로 createOrder + updateOrderStatus 사용
     */
    @Transactional
    fun executeOrder(walletId: Long, ordersRequest: OrdersRequest): Orders {
        val order = createOrder(walletId, ordersRequest) // 1. 신규 주문 생성 (PENDING)

        val walletResponse = order.user?.id?.let { userId ->
            val req = BuyCoinRequest(
                order.coin!!.id!!,
                order.wallet!!.id!!,
                order.price!!.multiply(order.quantity!!),
                order.quantity!!
            )
            walletService.processTransaction(
                userId,
                req,
                if (ordersRequest.tradeType == TradeType.BUY) TransactionType.BUY else TransactionType.SELL
            )
        }

        // 2. 상태 업데이트
        return if (walletResponse != null && walletResponse.statusCode.is2xxSuccessful) {
            updateOrderStatus(order.id!!, OrdersStatus.FILLED)
        } else {
            updateOrderStatus(order.id!!, OrdersStatus.FAILED, "거래 실패: ${walletResponse?.statusCode ?: "알 수 없음"}")
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
    fun cancelOrders(orderIds: List<Long>): List<OrderResponse> {
        val results = mutableListOf<OrderResponse>()

        orderIds.forEach { orderId ->
            val order = ordersRepository.findById(orderId)
                .orElseThrow { IllegalArgumentException("주문을 찾을 수 없습니다. ID: $orderId") }

            // 상태 검사 (대기 중인 주문만 취소 가능)
            order?.let {
                if (it.ordersStatus != OrdersStatus.PENDING) {
                    throw IllegalStateException("대기 중인 주문만 취소할 수 있습니다. ID: $orderId")
                }
            }

            order?.markCancelled() // 엔티티 상태 전환
            order?.let { results.add(OrderResponse.from(it)) }
        }

        return results
    }

    @Transactional
    fun createMockOrders() {
        if (ordersRepository.count() > 0) return

        // Wallet과 User는 이미 있다고 가정 (1번 지갑 꺼내오기)
        val wallet = walletRepository.findById(1L)
            .orElseThrow { RuntimeException("wallet not found") }
        val user = wallet.user ?: throw RuntimeException("user not found")

        // 코인 생성
        coinRepository.deleteAll()
        val coin1 = coinRepository.save(Coin("비트코인", "Bitcoin", "BTC"))
        val coin2 = coinRepository.save(Coin("이더리움", "Ethereum", "ETH"))
        val coin3 = coinRepository.save(Coin("리플", "Ripple", "XRP"))

        val baseDate = LocalDateTime.of(2025, 7, 25, 9, 0)

        val orders = (1..15).map { i ->
            val coin = when {
                i <= 5 -> coin1
                i <= 10 -> coin2
                else -> coin3
            }
            val tradeType = if (i % 2 == 0) TradeType.BUY else TradeType.SELL
            val method = if (i % 3 == 0) OrdersMethod.MARKET else OrdersMethod.LIMIT

            val status = when (i % 6) {
                0 -> OrdersStatus.PENDING
                1 -> OrdersStatus.FILLED
                2 -> OrdersStatus.CANCELLED
                3 -> OrdersStatus.FAILED
                4 -> OrdersStatus.EXPIRED
                else -> OrdersStatus.PARTIALLY_FILLED
            }

            Orders.builder()
                .user(user)
                .wallet(wallet)
                .coin(coin)
                .tradeType(tradeType)
                .ordersMethod(method)
                .quantity(BigDecimal((i % 5 + 1).toString())) // 1 ~ 5
                .price(BigDecimal(1_000_000L + (i * 50_000L)))
                .ordersStatus(status)
                .createdAt(baseDate.plusHours(i.toLong()))
                .build()
        }

        ordersRepository.saveAll(orders)
    }
}