package rolling_window

import kotlinx.serialization.Serializable

@Serializable
data class WSFrame(val params: Params) {
    @Serializable
    data class Params(val data: Data)
    @Serializable
    data class Data(val last_price: Double? = null, val timestamp: Long)
}
