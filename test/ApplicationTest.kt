package io.github.potatocurry

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.stringify
import kotlin.test.Test

@UnstableDefault
@ImplicitReflectionSerializer
class ApplicationTest {
    val json = Json(JsonConfiguration.Default)

    @Test
    fun autoTest() {
        withTestApplication({ module() }) {
            handleWebSocketConversation("/messages") { incoming, outgoing ->
                outgoing.send(
                    Frame.Text("""{
                      "channel": "channel1",
                      "text": "aaaaaaaa"
                    }""".trimIndent())
                )

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            val message = json.parse(Message.serializer(), text)
                            println(text)
                        }
                        else -> {
                            println("received ${frame.data}")
                        }
                    }
                }
            }
        }
    }

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
}
