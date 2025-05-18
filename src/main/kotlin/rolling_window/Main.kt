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


fun main(args: Array<String>) = runBlocking {
    val instrument = args.getOrNull(0) ?: "BTC-PERPETUAL"
    val windowSec = args.getOrNull(1)?.toIntOrNull() ?: 60
    val json = Json { ignoreUnknownKeys = true }

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
        val sub = buildJsonObject {
            put("jsonrpc", "2.0"); put("id", 42)
            put("method", "public/subscribe")
            putJsonObject("params") {
                putJsonArray("channels") { add("incremental_ticker.$instrument") }
            }
        }
        send(Frame.Text(sub.toString()))

        for (frame in incoming) if (frame is Frame.Text) {
            val root = json.parseToJsonElement(frame.readText()).jsonObject
            if (!root.containsKey("params")) continue
            val tick = json.decodeFromJsonElement<WSFrame>(root).params.data
            val price = tick.last_price ?: continue
            window.add(tick.timestamp, price)
            window.slope()?.let { trendFlow.value = it }
        }
    }
}
