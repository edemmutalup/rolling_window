package rolling_window

import kotlinx.serialization.Serializable

@Serializable
data class SubscribeRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 42,
    val method: String = "public/subscribe",
    val params: Params
) {
    @Serializable
    data class Params(val channels: List<String>)
}