package tools

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.jivesoftware.smack.chat2.*
import org.jivesoftware.smack.tcp.*
import org.jxmpp.jid.*
import org.jxmpp.jid.impl.*
import java.util.*
import javax.net.ssl.*

class SimplifiedXmppClient private constructor(
    val user: String,
    val password: String,
    val host: String,
    val port: Int,
    val domain: String
) {
    companion object {
        suspend fun connectTo(
            user: String,
            password: String,
            host: String,
            port: Int = 5222,
            domain: String = user.substringAfter('@', host)
        ): SimplifiedXmppClient {
            return SimplifiedXmppClient(user, password, host, port, domain).apply {
                connectAndLogin()
            }
        }
    }

    private val config = XMPPTCPConnectionConfiguration.builder()
        .setUsernameAndPassword(user, password)
        .setXmppDomain(domain)
        .setHost(host)
        .setPort(port)
        .setCustomSSLContext(SSLContext.getDefault())
        .addEnabledSaslMechanism("PLAIN")
        .build()

    private val connection = XMPPTCPConnection(config)
    private val chatManager = ChatManager.getInstanceFor(connection)

    private suspend fun connectAndLogin() {
        //connection.addStanzaInterceptor({ println(it) }, { true })
        connection.connect()
        connection.login()
    }

    data class From(private val from: EntityBareJid)

    data class Message(private val message: org.jivesoftware.smack.packet.Message) {
        val from get() = message.from.toString()
        val body get() = message.body
    }

    inner class Chat(private val from: EntityBareJid, private val chat: org.jivesoftware.smack.chat2.Chat) :
        CommunicationChannel {
        internal val _channel = Channel<Message>(Channel.UNLIMITED)
        val channel get() = _channel as ReceiveChannel<Message>

        override suspend fun receive() = channel.receive().body

        operator fun iterator(): ChannelIterator<Message> = channel.iterator()

        override suspend fun send(message: String) {
            chat.send(message)
            //connection.sendStanza(Message().apply {
            //    this.body = message
            //    this.type = org.jivesoftware.smack.packet.Message.Type.chat
            //    this.to = this@Chat.from
            //})
        }
    }

    private val chats = WeakHashMap<org.jivesoftware.smack.chat2.Chat, Chat>()

    suspend fun onChat(callback: suspend Chat.() -> Unit) {
        //println("onChat")
        chatManager.setXhmtlImEnabled(true)
        chatManager.addIncomingListener { from, message, chat ->
            //println("RECEIVED MESSAGE $from, $message, $chat")
            val rchat = chats.getOrPut(chat) {
                val rchat = Chat(from, chat)
                launch {
                    callback(rchat)
                }
                rchat
            }
            rchat._channel.offer(Message(message))
        }
    }

    suspend fun send(user: String, message: String) {
        val chat = chatManager.chatWith(JidCreate.entityBareFrom(user))
        chat.send(message)
    }

    suspend fun loop() {
        while (true) {
            delay(100)
        }
    }

    suspend fun disconnect() {
        connection.disconnect()
    }
}
