package io.github.potatocurry

import io.github.potatocurry.Channels.name
import io.github.potatocurry.Manager.channels
import io.github.potatocurry.Manager.getLastMessages
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.cio.websocket.*
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.stringify
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration

val sessions = mutableListOf<WebSocketSession>()

fun main(args: Array<String>) = EngineMain.main(args)

@UnstableDefault
@ImplicitReflectionSerializer
fun Application.module() {
    Database.connect("jdbc:postgresql://localhost:5432/postgres", "org.postgresql.Driver", "postgres", "postgres")
    transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.createMissingTablesAndColumns(Channels, Users, Messages)
        importChannels().forEach { channelInfo ->
            if (Channels.select { name.eq(channelInfo.first) }.empty())
                Channels.insert {
                    it[name] = channelInfo.first
                    it[description] = channelInfo.second
                }
        }
    }

    install(CallLogging)

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val json = Json(JsonConfiguration.Default)

    routing {
        get("info") {
            call.respondText("server info")
        }

        route("channels") {
            get {
                call.respondText(json.stringify(channels))
            }

            get("{channel}") { // TODO: Add size parameter
                val channelName = call.parameters["channel"]
                val channel = channels.single { it.name == channelName }
                call.respondText(json.stringify(Message.serializer().list, getLastMessages(channel)))
            }
        }

        webSocket("messages") {
            sessions += this

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        if (text.equals("bye", true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                        val message = json.parse(Message.serializer(), text)
                        Manager.insertMessage(message)
                        broadcastMessage(json.stringify(message))
                    }
                    else -> {
                        println("received ${frame.data}")
                    }
                }
            }
        }
    }
}

fun importChannels(): List<Pair<String, String>> {
    return listOf("channel1" to "This is channel 1", "channel2" to "This is channel 2")
}

suspend fun broadcastMessage(text: String) {
    for (websocket in sessions) // launch coroutine to avoid blocking?
        websocket.outgoing.send(Frame.Text(text))
}
