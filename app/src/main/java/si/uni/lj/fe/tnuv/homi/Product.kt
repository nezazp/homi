package si.uni.lj.fe.tnuv.homi

data class Product(
    val id: Int,
    val name: String,
    val isBought: Boolean = false,
    val downvotes: Int = 0,
    val downvoters: List<String> = emptyList(), // List of user IDs who downvoted
    val timestamp: Long,
    val addedBy: String
)