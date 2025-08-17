package si.uni.lj.fe.tnuv.homi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class ProductAdapter(
    private var products: List<Product>,
    private var onBoughtClick: (Product, Int) -> Unit, // Modified to include position
    private var onDownvoteClick: (Product) -> Unit,
    private val groupId: String,
    private val currentUserUid: String
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val database = Firebase.database.reference

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.productName)
        val addedByText: TextView = itemView.findViewById(R.id.productAddedBy)
        val timestampText: TextView = itemView.findViewById(R.id.productTimestamp)
        val downvotesText: TextView = itemView.findViewById(R.id.productDownvotes)
        val boughtButton: Button = itemView.findViewById(R.id.boughtButton)
        val downvoteButton: Button = itemView.findViewById(R.id.downvoteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.nameText.text = product.name
        holder.addedByText.text = "Added by: ${product.addedBy}"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.timestampText.text = dateFormat.format(Date(product.timestamp))
        holder.downvotesText.text = "Downvotes: ${product.downvotes}"

        holder.boughtButton.text = if (product.isBought) "Bought" else "Not Bought"
        val context = holder.itemView.context
        holder.boughtButton.backgroundTintList = ContextCompat.getColorStateList(
            context,
            if (product.isBought) R.color.button_selected else R.color.colorPrimary
        )
        holder.boughtButton.setOnClickListener {
            if (!product.isBought) {
                // Show dialog when clicking "Not Bought" to mark as "Bought"
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_price_input, null)
                val priceInput = dialogView.findViewById<EditText>(R.id.priceInput)
                val takePhotoButton = dialogView.findViewById<Button>(R.id.takePhotoButton)

                val dialog = AlertDialog.Builder(context)
                    .setTitle("Enter Price")
                    .setView(dialogView)
                    .setPositiveButton("Confirm") { _, _ ->
                        val priceText = priceInput.text.toString()
                        val price = priceText.toDoubleOrNull()
                        if (price != null) {
                            // Convert price to cents for spentAmount
                            val priceInCents = (price * 100).toLong()
                            // Update user's spentAmount in Firebase
                            database.child("users").child(currentUserUid).get()
                                .addOnSuccessListener { snapshot ->
                                    val user = snapshot.getValue(User::class.java)
                                    val currentSpentAmount = user?.spentAmount ?: 0L
                                    val newSpentAmount = currentSpentAmount + priceInCents
                                    database.child("users").child(currentUserUid)
                                        .child("spentAmount").setValue(newSpentAmount)
                                        .addOnSuccessListener {
                                            // Update product to mark as bought with price
                                            val updatedProduct = product.copy(isBought = true, price = price)
                                            // Update local products list
                                            val mutableProducts = products.toMutableList()
                                            mutableProducts[position] = updatedProduct
                                            products = mutableProducts
                                            notifyItemChanged(position) // Refresh only this item
                                            onBoughtClick(updatedProduct, position)
                                            Toast.makeText(context, "Price entered: $price", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { error ->
                                            Toast.makeText(context, "Failed to update spent amount: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(context, "Failed to fetch user data: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .create()

                takePhotoButton.setOnClickListener {
                    Toast.makeText(context, "Take Photo clicked", Toast.LENGTH_SHORT).show()
                    // Camera functionality to be implemented separately
                }

                dialog.show()
            } else {
                // Directly mark as not bought without dialog
                val updatedProduct = product.copy(isBought = false, price = null)
                val mutableProducts = products.toMutableList()
                mutableProducts[position] = updatedProduct
                products = mutableProducts
                notifyItemChanged(position)
                onBoughtClick(updatedProduct, position)
            }
        }

        holder.downvoteButton.setOnClickListener { onDownvoteClick(product) }

        // Existing delete dialog
        holder.itemView.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete ${product.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    ProductStore.deleteProduct(database, groupId, product.id) {
                        ProductStore.getProducts(database, groupId) { updatedProducts ->
                            updateProducts(updatedProducts)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        holder.itemView.contentDescription = "Product: ${product.name}, added by ${product.addedBy} on ${dateFormat.format(Date(product.timestamp))}, ${if (product.isBought) "bought" else "not bought"}, ${product.downvotes} downvotes"
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    fun setCallbacks(onBoughtClick: (Product, Int) -> Unit, onDownvoteClick: (Product) -> Unit) {
        this.onBoughtClick = onBoughtClick
        this.onDownvoteClick = onDownvoteClick
    }
}