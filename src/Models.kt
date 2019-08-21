package io.github.potatocurry

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
data class Message(
//    val publicKey: String,
    val channel: String,
    val text: String
)
