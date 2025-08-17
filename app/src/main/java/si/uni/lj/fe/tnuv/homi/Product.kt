package si.uni.lj.fe.tnuv.homi

data class Product(
    val id: String,
    val name: String,
    val isBought: Boolean = false,
    val downvotes: Int = 0,
    val downvoters: List<String> = emptyList(),
    val timestamp: Long,
    val addedBy: String,
    val price: Double? = null // Add price field
)