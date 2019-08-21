package io.github.potatocurry

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.stringify
import org.joda.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@UnstableDefault
@ImplicitReflectionSerializer
class ApplicationTest {
    private val json = Json(JsonConfiguration.Default)

    @Test
    fun channelsTest() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/channels").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val channelsJson = assertNotNull(response.content)
                val channels = json.parse(Channel.serializer().list, channelsJson)
                channels.forEach { channel ->
                    handleRequest(HttpMethod.Get, "/channels/${channel.name}").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val messagesJson = assertNotNull(response.content)
                        val messages = json.parse(Message.serializer().list, messagesJson)
                    }
                }
            }
        }
    }

    @Test
    fun messagesTest() {
        withTestApplication({ module() }) {
            handleWebSocketConversation("/messages") { incoming, outgoing ->
                val dateText = "This is a message sent at ${DateTime.now()}"
                val outMessage = Message("channel1", dateText)
                val outJson = json.stringify(outMessage)
                outgoing.send(Frame.Text(outJson))

                val inJson = ((incoming.receive()) as Frame.Text).readText()
                val inMessage = json.parse(Message.serializer(), inJson)
                assertEquals(dateText, inMessage.text)
            }
        }
    }
}
