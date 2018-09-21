package tools

interface CommunicationChannel {
    suspend fun receive(): String
    suspend fun send(message: String)
}
