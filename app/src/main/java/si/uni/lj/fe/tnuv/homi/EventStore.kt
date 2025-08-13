package si.uni.lj.fe.tnuv.homi

object EventStore {
    private val events = mutableListOf<Event>()

    @Synchronized
    fun addEvent(event: Event) {
        events.add(event)
    }

    @Synchronized
    fun removeEvent(event: Event) {
        events.remove(event)
    }

    @Synchronized
    fun getEvents(): List<Event> {
        return events.toList()
    }
}