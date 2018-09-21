package tools
open class BaseScript(private val chat: CommunicationChannel) {
    suspend fun read() = chat.receive()
    suspend fun send(text: String) {
        //println("SEND $text")
        chat.send(text)
    }
    class ChangeScene(val callback: suspend () -> Unit) : Throwable()
    suspend fun change(callback: suspend () -> Unit) {
        throw ChangeScene(callback)
    }

    class Matcher(val matches: List<Any>, val callback: suspend () -> Unit) {
        private fun tryMatch(rule: Any?, subject: Set<String>): String? {
            // Any of them
            if (rule is Iterable<*>) {
                for (it in rule) {
                    val result = tryMatch(it, subject)
                    if (result != null) return result
                }
                return null
            } else {
                val v = rule.toString().toLowerCase()
                return if (v in subject) v else null
            }
        }

        fun matches(subject: Set<String>): List<String>? {
            val out = arrayListOf<String>()
            for (match in matches) {
                out += tryMatch(match, subject) ?: return null
            }
            return out
        }
    }

    class MatchBuilder {
        val matchers = arrayListOf<Matcher>()
        val defaultMatchers = arrayListOf<Matcher>()
        val allMatchers get() = matchers + defaultMatchers.reversed()

        fun match(vararg matches: Any, callback: suspend () -> Unit) {
            matchers += Matcher(matches.toList(), callback)
        }
        fun default(callback: suspend () -> Unit) {
            defaultMatchers += Matcher(listOf(), callback)
        }
    }

    open fun MatchBuilder.defaultMatches() {
    }

    suspend fun matchRead(matches: MatchBuilder.() -> Unit) = match(read(), matches)

    suspend fun match(line: String, matches: MatchBuilder.() -> Unit) {
        val parts = line.toLowerCase().split(Regex("\\b+")).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val matchBuilder = MatchBuilder().apply { defaultMatches() }.apply(matches)
        for (match in matchBuilder.allMatchers) {
            val result = match.matches(parts)
            if (result != null) {
                println("MATCHED: $parts -> $result")
                //change(match.callback)
                match.callback()
                break
            }
        }
    }

    suspend fun execute(callback: suspend () -> Unit) {
        var ccallback = callback
        loop@while (true) {
            try {
                ccallback()
                continue@loop
            } catch (e: ChangeScene) {
                ccallback = e.callback
                continue@loop
            }
        }
    }

    protected open suspend fun entry() {
    }

    suspend fun start() {
        execute { entry() }
    }
}
