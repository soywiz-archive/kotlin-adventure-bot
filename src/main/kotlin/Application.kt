import kotlinx.coroutines.*
import tools.*

fun getGlobalProp(vararg names: String): String {
    for (name in names) {
        val res = System.getProperty(name) ?: System.getenv(name)
            if (res != null) return res
    }
    error("Required any of ${names.joinToString(", ")} property or environment variable")
}

fun main(args: Array<String>): Unit {
    runBlocking {
        val user = getGlobalProp("xmpp.user", "XMPP_USER")
        val pass = getGlobalProp("xmpp.pass", "XMPP_PASS")
        val chat = SimplifiedXmppClient.connectTo(user, pass, host = "xmpp.l.google.com")
        println("READY $user")
        chat.onChat {
            receive()
            MyScript(this).start()
        }
        chat.loop()
    }
}
