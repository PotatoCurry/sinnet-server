package io.github.potatocurry

import io.github.potatocurry.Channels.name
import io.ktor.http.ContentType
import org.jetbrains.exposed.sql.SortOrder
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

    val messages
        get() = transaction {
            Messages.selectAll().orderBy(Messages.time to SortOrder.ASC).map {
                Message(
                    Channels.select { Channels.id eq it[Messages.channelId] }.single()[Channels.name],
                    it[Messages.signedText]
                )
            }
        }

    fun insertMessage(message: Message): InsertStatement<Number> {
        return transaction {
            Messages.insert {
                it[channelId] = Channels.select { name eq message.channel }.single()[Channels.id]
//                it[userId] = Users.select { publicKey eq message.publicKey }.single()[Users.id]
                it[time] = DateTime.now()
                it[signedText] = message.text
            }
        }
    }
}
