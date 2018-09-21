import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.junit.Test
import tools.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testHappyPath() {
        testCommands(
            { MyScript(it).apply { hasKey = false } },
            listOf(
                "go to the left",
                "get key",
                "return",
                "open right door"
            )
        ) { log ->
            assertEquals(
                """
                    You are in a room with two doors: one to your left, and the other to your right
                    In this room there is a key
                    You got a key!
                    This room is empty
                    You are in a room with two doors: one to your left, and the other to your right
                    You opened the door!
                    Congratulations, you managed to get to the right room!
                    You are in a room with two doors: one to your left, and the other to your right
                """.trimIndent(),
                log
            )
        }
    }

    private fun testCommands(build: (CommunicationChannel) -> BaseScript, commands: List<String>, callback: (String) -> Unit) {
        runBlocking {
            val log = arrayListOf<String>()
            val channel = produce<String> {
                for (command in commands) {
                    channel.send(command)
                }
            }

            try {
                build(object : CommunicationChannel {
                    override suspend fun receive(): String = channel.receive()
                    override suspend fun send(message: String) {
                        log += message
                        println(message)
                    }
                }).start()
            } catch (e: ClosedReceiveChannelException) {
            }

            callback(log.joinToString("\n"))
        }
    }
}
