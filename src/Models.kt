package io.github.potatocurry

import io.github.potatocurry.Messages.channelId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

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
