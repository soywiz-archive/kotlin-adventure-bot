import tools.*

class MyScript(chat: CommunicationChannel) : BaseScript(chat) {
    var hasKey = false

    override fun MatchBuilder.defaultMatches() {
        match(Objects.inventory) {
            if (hasKey) {
                send("You have a key")
            } else {
                send("You don't have anything")
            }
        }
        default {
            send("I'm sorry, couldn't understand you.")
        }
    }

    override suspend fun entry() {
        send("You are in a room with two doors: one to your left, and the other to your right")
        matchRead {
            match(Action.GO_OR_OPEN, Places.left) { change(::left) }
            match(Action.GO, Places.right) {
                send("The door is locked")
            }
            match(Action.OPEN, Places.right) {
                if (hasKey) {
                    send("You opened the door!")
                    change(::right)
                } else {
                    send("You don't have any key that fits the lock")
                }
            }
            default {
                send("I'm sorry, couldn't understand you. You can *go* to the left or the right.")
                entry()
            }
        }
    }

    suspend fun left() {
        if (hasKey) {
            send("This room is empty")
        } else {
            send("In this room there is a key")
        }
        matchRead {
            match(Action.PICK, Objects.key) {
                send("You got a key!")
                hasKey = true
                left()
            }
            match(Action.GO_BACK) { entry() }
            default { left() }
        }
    }

    suspend fun right() {
        send("Congratulations, you managed to get to the right room!")
        change(::entry)
    }
}


enum class Action(vararg val aliases: String) : Iterable<String> by aliases.toList() {
    GO("go", "move"),
    OPEN("open", "opens"),
    PICK("get", "pick"),
    GO_BACK("back", "return"),
    ;

    companion object {
        val GO_OR_OPEN = listOf(GO, OPEN)
    }
}

object Objects {
    val inventory = "inventory"
    val key = "key"
}

object Places {
    val left = "left"
    val right = "right"
}

