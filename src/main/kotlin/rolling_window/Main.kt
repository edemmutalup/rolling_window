package rolling_window

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.util.logging.Level
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger("rolling_window")


fun main(args: Array<String>) = runBlocking {
    val instrument = args.getOrNull(0) ?: "BTC-PERPETUAL"
    val windowSec = args.getOrNull(1)?.toIntOrNull() ?: 60
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val client = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) { json(json) }
    }

    val window = RollingWindow(windowSec)
    val trendFlow = MutableStateFlow<Double?>(null)

    launch {
        @OptIn(kotlinx.coroutines.ObsoleteCoroutinesApi::class)
        val tick = ticker(delayMillis = 5000, initialDelayMillis = 5000)
        for (ignored in tick) {
            val slope = trendFlow.value ?: continue
            println(
                "[${instrument}] slope = ${
                    "%.6f".format(slope)
                } USD/sec (window ${windowSec}s)"
            )
        }
    }

    client.webSocket("wss://test.deribit.com/ws/api/v2") {
        val request = SubscribeRequest(
            params = SubscribeRequest.Params(
                channels = listOf("incremental_ticker.$instrument")
            )
        )
        val text = json.encodeToString(request)
        send(Frame.Text(text))

        incoming.consumeAsFlow().collect { frame ->
            if (frame is Frame.Text) {
                try {
                    val root = json.parseToJsonElement(frame.readText()).jsonObject
                    if (!root.containsKey("params")) return@collect
                    val tick = json.decodeFromJsonElement<WSFrame>(root).params.data
                    val price = tick.last_price ?: return@collect
                    window.add(tick.timestamp, price)
                    window.slope()?.let { trendFlow.value = it }
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Failed to parse WebSocket message", e)
                }
            }
        }
    }
}
