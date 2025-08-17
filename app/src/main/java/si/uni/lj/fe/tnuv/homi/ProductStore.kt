package si.uni.lj.fe.tnuv.homi

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

object ProductStore {
    fun getProducts(database: DatabaseReference, groupId: String, callback: (List<Product>) -> Unit) {
        database.child("groups").child(groupId).child("products")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val products = mutableListOf<Product>()
                    for (child in snapshot.children) {
                        val name = child.child("name").value as? String ?: continue
                        val isBought = child.child("isBought").value as? Boolean ?: false
                        val downvotes = (child.child("downvotes").value as? Long)?.toInt() ?: 0
                        val downvotersRaw = child.child("downvoters").value as? List<*>?
                        val downvoters = downvotersRaw?.filterIsInstance<String>() ?: emptyList()
                        val timestamp = child.child("timestamp").value as? Long ?: 0
                        val addedBy = child.child("addedBy").value as? String ?: ""
                        val product = Product(
                            id = child.key!!,
                            name = name,
                            isBought = isBought,
                            downvotes = downvotes,
                            downvoters = downvoters,
                            timestamp = timestamp,
                            addedBy = addedBy,
                            price = child.child("price").value as? Double // Retrieve price
                        )
                        products.add(product)
                    }
                    callback(products)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList()) // Handle error by returning empty list
                }
            })
    }

    fun addProduct(database: DatabaseReference, groupId: String, product: Product, callback: () -> Unit = {}) {
        val key = database.child("groups").child(groupId).child("products").push().key ?: return
        val productData = mapOf(
            "name" to product.name,
            "isBought" to product.isBought,
            "downvotes" to product.downvotes,
            "downvoters" to product.downvoters,
            "timestamp" to product.timestamp,
            "addedBy" to product.addedBy,
            "price" to product.price // Add price
        )
        database.child("groups").child(groupId).child("products").child(key).setValue(productData)
            .addOnSuccessListener { callback() }
    }

    fun updateProduct(database: DatabaseReference, groupId: String, updatedProduct: Product, callback: () -> Unit = {}) {
        val productData = mapOf(
            "name" to updatedProduct.name,
            "isBought" to updatedProduct.isBought,
            "downvotes" to updatedProduct.downvotes,
            "downvoters" to updatedProduct.downvoters,
            "timestamp" to updatedProduct.timestamp,
            "addedBy" to updatedProduct.addedBy,
            "price" to updatedProduct.price // Add price
        )
        database.child("groups").child(groupId).child("products").child(updatedProduct.id)
            .setValue(productData)
            .addOnSuccessListener { callback() }
    }

    fun deleteProduct(database: DatabaseReference, groupId: String, productId: String, callback: () -> Unit = {}) {
        database.child("groups").child(groupId).child("products").child(productId)
            .removeValue()
            .addOnSuccessListener { callback() }
    }
}