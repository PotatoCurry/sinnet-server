package io.github.potatocurry

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import java.awt.Rectangle
import java.awt.Shape
import kotlin.reflect.KClass

data class Channel(
    val id: Int,
    val name: String,
    val description: String
)

// Use for displaying stuff in interface - remove from here?
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val publicKey: String
)

//@TypeFor(field = "type", adapter = TransmissionAdapter::class)
//sealed class Transmission(val type: String)
//
//class TransmissionAdapter: TypeAdapter<Transmission> {
//    override fun classFor(type: Any): KClass<out Transmission> = when (type as String) {
//        "message" -> Message::class
//        else -> throw IllegalArgumentException("Unknown type $type")
//    }
//}

data class Message(
//    val publicKey: String,
    val channel: String,
    val text: String
) //: Transmission("message")
