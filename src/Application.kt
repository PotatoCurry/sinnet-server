package io.github.potatocurry

import com.beust.klaxon.FieldRenamer
import com.beust.klaxon.Klaxon
import io.github.potatocurry.Channels.name
import io.github.potatocurry.Manager.channels
import io.github.potatocurry.Manager.getLastMessages
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.logging.Logger

val logger: Logger = Logger.getLogger("io.github.potatocurry")
val sessions = mutableListOf<WebSocketSession>()

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    Database.connect(System.getenv("JDBC_DATABASE_URL"), "org.postgresql.Driver")
    transaction {
        addLogger(Slf4jSqlDebugLogger)

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

    val klaxon = Klaxon().fieldRenamer(
        object: FieldRenamer {
            override fun toJson(fieldName: String) = FieldRenamer.camelToUnderscores(fieldName)
            override fun fromJson(fieldName: String) = FieldRenamer.underscoreToCamel(fieldName)
        }
    )

    routing {
        get("info") {
            call.respondText("server info")
        }

        route("channels") {
            get {
                call.respondText(klaxon.toJsonString(channels))
            }

            get("{channel}") {
                val channelName = call.parameters["channel"]
                val channel = channels.single { it.name == channelName }
                val limit = call.request.queryParameters["limit"]?.toInt()
                val lastMessages = if (limit == null)
                    getLastMessages(channel)
                else
                    getLastMessages(channel, limit)
                call.respondText(klaxon.toJsonString(lastMessages))
            }
        }

        webSocket("messages") {
            sessions += this

            try {
                for (frame in incoming) when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val message = klaxon.parse<Message>(text)!! // if null log error
                        Manager.insertMessage(message)
                        val messageJson = klaxon.toJsonString(message)
                        println(messageJson)
                        broadcastMessage(messageJson)
                    }
                    else -> {
                        println("Received unusual frame ${frame.data}") // warn about unusual activity
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                sessions.remove(this)
                println("disconnected with error")
            } finally {
                sessions.remove(this)
                println("disconnected normally")
            }
        }
    }
}

fun importChannels(): List<Pair<String, String>> {
    return listOf("channel1" to "This is channel 1", "channel2" to "This is channel 2")
}

suspend fun broadcastMessage(text: String) {
    for (websocket in sessions) { // launch coroutine to avoid blocking?
        websocket.outgoing.send(Frame.Text(text))
        println("broadcasted $text")
    }
}
