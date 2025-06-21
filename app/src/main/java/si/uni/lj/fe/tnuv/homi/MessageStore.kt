package si.uni.lj.fe.tnuv.homi

object MessageStore {
    private val messages = mutableListOf<Message>()

    fun getMessages(): List<Message> = messages.toList()

    fun addMessage(message: Message) {
        messages.add(message)
    }
}