package io.github.potatocurry

import org.jetbrains.exposed.dao.IntIdTable

object Channels : IntIdTable() {
    val name = varchar("name", 20)
    val description = text("description")
}

object Messages : IntIdTable() {
    val channelId = (integer("channel_id")
        .references(Channels.id))
        .entityId()
//    val userId = (integer("user_id")
//        .references(Users.id))
//        .entityId()
    val time = datetime("time")
    val signedText = text("text")
}

object Users : IntIdTable() {
    val publicKey = text("public_key")
}
