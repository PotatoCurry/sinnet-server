package io.github.potatocurry

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlin.reflect.KClass

data class Channel(
    val id: Int,
    val name: String,
    val description: String
)

@TypeFor("type", TransmissionAdapter::class)
sealed class Transmission(val type: String)

class TransmissionAdapter: TypeAdapter<Transmission> {
    override fun classFor(type: Any): KClass<out Transmission> = when (type as String) {
        "user_join" -> UserJoin::class
        "user_leave" -> UserLeave::class
        "message" -> Message::class
        else -> throw IllegalArgumentException("Received unknown type $type")
    }
}

data class UserJoin(
    val publicKey: String
) : Transmission("user_join")

data class UserLeave(
    val publicKey: String
) : Transmission("user_leave")

data class Message(
    val channel: String,
    val text: String
) : Transmission("message")
