package com.back.back9.domain.websocket.service

import com.back.back9.domain.websocket.vo.CandleInterval
import com.back.back9.global.redis.service.RedisService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.client.WebSocketConnectionManager
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.min

@Component
class WebSocketClient(
    private val redisService: RedisService,
    private val objectMapper: ObjectMapper,
    private val coinListProvider: DatabaseCoinListProvider
) : TextWebSocketHandler() {

    private val logger = KotlinLogging.logger {}

    private lateinit var manager: WebSocketConnectionManager
    @Volatile private var session: WebSocketSession? = null

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var reconnectAttempt: Int = 0
    private val maxReconnectDelayMs = 30_000L
    private val subscribeChunkSize = 80
    private val pingInterval = Duration.ofSeconds(20)

    @PostConstruct
    fun connect() {
        val client = StandardWebSocketClient()
        val url = "wss://api.upbit.com/websocket/v1"

        manager = WebSocketConnectionManager(client, this, url).apply {
            isAutoStartup = true
            start()
        }

        scheduler.scheduleAtFixedRate({
            runCatching {
                session?.takeIf { it.isOpen }?.sendMessage(
                    PingMessage(ByteBuffer.wrap("ping".toByteArray(StandardCharsets.UTF_8)))
                )
            }.onFailure { e -> logger.warn(e) { "Ping 전송 실패" } }
        }, pingInterval.seconds, pingInterval.seconds, TimeUnit.SECONDS)
    }

    @PreDestroy
    fun disconnect() {
        if (::manager.isInitialized && manager.isRunning) {
            manager.stop()
        }
        runCatching { session?.close() }
        scheduler.shutdownNow()
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        this.session = session
        reconnectAttempt = 0

        runCatching {
            val codes = coinListProvider.getMarketCodes()
            if (codes.isEmpty()) return

            val chunks = codes.chunked(subscribeChunkSize)
            chunks.forEachIndexed { idx, chunk ->
                val payload = objectMapper.writeValueAsString(
                    listOf(
                        mapOf("ticket" to "realtime-ticker-$idx"),
                        mapOf("type" to "candle.1s", "codes" to chunk),
                        mapOf("type" to "candle.1m", "codes" to chunk),
                        mapOf("format" to "SIMPLE")
                    )
                )
                session.sendMessage(TextMessage(payload))
            }
        }.onFailure { e ->
            logger.error(e) { "WebSocket 구독 요청 실패" }
            scheduleReconnect()
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        parseAndSaveCandle(message.payload)
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val payload = StandardCharsets.UTF_8.decode(message.payload).toString()
        parseAndSaveCandle(payload)
    }

    override fun handlePongMessage(session: WebSocketSession, message: PongMessage) {
        // 필요 시 pong 수신 로깅
    }

    private fun parseAndSaveCandle(payload: String) {
        synchronized(this) {
            runCatching {
                val root: JsonNode = objectMapper.readTree(payload)
                val type = root.path("ty").asText()
                if (!type.startsWith("candle.")) return@runCatching

                val market = root.path("cd").asText()
                val interval = CandleInterval.fromWebSocketType(type)

                val candleNode = objectMapper.createObjectNode().apply {
                    put("market", market)
                    put("candle_date_time_kst", root.path("cdttmk").asText(""))
                    put("opening_price", root.path("op").asDouble(0.0))
                    put("high_price", root.path("hp").asDouble(0.0))
                    put("low_price", root.path("lp").asDouble(0.0))
                    put("trade_price", root.path("tp").asDouble(0.0))
                    put("timestamp", root.path("tms").asLong(0))
                    put("candle_acc_trade_price", root.path("catp").asDouble(0.0))
                    put("candle_acc_trade_volume", root.path("catv").asDouble(0.0))
                }

                redisService.saveCandle(interval, market, candleNode)
                redisService.saveLatestCandle(market, candleNode)
            }.onFailure { e ->
                logger.error(e) { "WebSocket 메시지 파싱 실패" }
            }
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error(exception) { "WebSocket 전송 오류 발생" }
        scheduleReconnect()
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        this.session = null
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (!::manager.isInitialized) return
        val delay = min((1 shl reconnectAttempt) * 1000L, maxReconnectDelayMs)
        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(15)

        scheduler.schedule({
            runCatching {
                if (!manager.isRunning) {
                    manager.start()
                }
            }.onFailure { e ->
                logger.error(e) { "WebSocket 재연결 실패" }
            }
        }, delay, TimeUnit.MILLISECONDS)
    }
}
