package si.uni.lj.fe.tnuv.homi



import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.Button

import android.widget.TextView

import androidx.core.content.ContextCompat

import androidx.recyclerview.widget.RecyclerView

import java.text.SimpleDateFormat

import java.util.*



class ProductAdapter(

    private var products: List<Product>,

    private var onBoughtClick: (Product) -> Unit,

    private var onDownvoteClick: (Product) -> Unit

) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {



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

        holder.boughtButton.setOnClickListener { onBoughtClick(product) }



        holder.downvoteButton.setOnClickListener { onDownvoteClick(product) }



        holder.itemView.contentDescription = "Product: ${product.name}, added by ${product.addedBy} on ${dateFormat.format(Date(product.timestamp))}, ${if (product.isBought) "bought" else "not bought"}, ${product.downvotes} downvotes"

    }



    override fun getItemCount(): Int = products.size



    fun updateProducts(newProducts: List<Product>) {

        products = newProducts

        notifyDataSetChanged()

    }

    fun setCallbacks(onBoughtClick: (Product) -> Unit, onDownvoteClick: (Product) -> Unit) {

        this.onBoughtClick = onBoughtClick

        this.onDownvoteClick = onDownvoteClick

    }

}