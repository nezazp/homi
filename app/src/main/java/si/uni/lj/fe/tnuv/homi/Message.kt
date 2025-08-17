package si.uni.lj.fe.tnuv.homi

data class Message(
    val username: String = "",
    val content: String = "",
    val timestamp: Long = 0L
) {
    // No-argument constructor required by Firebase
    constructor() : this("", "", 0L)
}