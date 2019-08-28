package io.github.potatocurry

import com.beust.klaxon.FieldRenamer
import com.beust.klaxon.Klaxon
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import org.joda.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@UnstableDefault
@ImplicitReflectionSerializer
class ApplicationTest {
    val klaxon = Klaxon().fieldRenamer(
        object: FieldRenamer {
            override fun toJson(fieldName: String) = FieldRenamer.camelToUnderscores(fieldName)
            override fun fromJson(fieldName: String) = FieldRenamer.underscoreToCamel(fieldName)
        }
    )

    @Test
    fun channelsTest() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/channels").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val channelsJson = assertNotNull(response.content)
                println(channelsJson)
                val channels = klaxon.parseArray<Channel>(channelsJson)
                assertNotNull(channels)
                channels.forEach { channel ->
                    handleRequest(HttpMethod.Get, "/channels/${channel.name}").apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        val messagesJson = assertNotNull(response.content)
                        val messages = klaxon.parse<List<Message>>(messagesJson)
                        assertNotNull(messages)
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
                val outJson = klaxon.toJsonString(outMessage)
                outgoing.send(Frame.Text(outJson))

                val inJson = ((incoming.receive()) as Frame.Text).readText()
                val inMessage = klaxon.parse<Message>(inJson)
                assertNotNull(inMessage)
                assertEquals(dateText, inMessage.text)
            }
        }
    }
}
