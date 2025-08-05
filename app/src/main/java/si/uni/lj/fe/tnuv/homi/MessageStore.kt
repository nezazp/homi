package si.uni.lj.fe.tnuv.homi

object MessageStore {
    private val messages = mutableListOf<Message>()

    @Synchronized
    fun addMessage(message: Message) {
        messages.add(message)
    }

    @Synchronized
    fun getMessages(): List<Message> {
        return messages.toList()
    }
}