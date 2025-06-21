package si.uni.lj.fe.tnuv.homi

object ProductStore {
    private val products = mutableListOf<Product>()
    private var idCounter = 0

    fun getProducts(): List<Product> = products.toList()

    fun addProduct(product: Product) {
        products.add(product.copy(id = idCounter++, downvoters = emptyList()))
    }

    fun updateProduct(updatedProduct: Product) {
        val index = products.indexOfFirst { it.id == updatedProduct.id }
        if (index != -1) {
            products[index] = updatedProduct
        }
    }
}