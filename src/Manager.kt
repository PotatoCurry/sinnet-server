package io.github.potatocurry

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Manager {
    val channels: List<Channel>
        get() = transaction {
            Channels.selectAll().map {
                Channel(
                    it[Channels.id].value,
                    it[Channels.name],
                    it[Channels.description]
                )
            }
        }

    fun getLastMessages(channel: Channel, limit: Int = 50): List<Message> { // switch orderby asc and limit operation order?
        return transaction {
            val messagesRaw = Messages.select { Messages.channelId.eq(channel.id) }.limit(limit).toList()
            messagesRaw.map {
                 Message(
                     channel.name,
                     it[Messages.signedText]
                 )
            }
        }
    }

    fun insertMessage(message: Message): InsertStatement<Number> {
        return transaction {
            Messages.insert {
                it[channelId] = Channels.select { Channels.name eq message.channel }.single()[Channels.id]
//                it[userId] = Users.select { publicKey eq message.publicKey }.single()[Users.id]
                it[time] = DateTime.now()
                it[signedText] = message.text
            }
        }
    }
}
