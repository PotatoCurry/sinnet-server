package io.github.potatocurry

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.launch
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
    val json = Json(JsonConfiguration.Default)

    @Test
    fun autoTest() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/info").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotNull(response.content)
            }
            handleRequest(HttpMethod.Get, "/channels").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = assertNotNull(response.content)
                val channels = json.parse(Channel.serializer().list, content)
                assertEquals(2, channels.size)
            }

            // Get message history
//            val messageHistory = json.parse(Message.serializer().list, )

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

    /*
    @Test
    fun manualTests() {
        withTestApplication({ module() }) {
            handleWebSocketConversation("/messages") { incoming, outgoing ->
                launch {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val inText = frame.readText()
                                val inMessage = json.parse(Message.serializer(), inText)
                                println(inText)
                            }
                            else -> {
                                println("received ${frame.data}")
                            }
                        }
                    }
                }

                while (true) {
                    val text = readLine()!!
                    val message = Message(
                        "channel1",
                        text
                    )
                    outgoing.send(Frame.Text(json.stringify(message)))
                }
            }
        }
    }
    */
}
