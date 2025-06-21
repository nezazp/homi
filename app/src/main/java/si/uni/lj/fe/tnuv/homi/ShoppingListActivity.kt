package si.uni.lj.fe.tnuv.homi



import android.content.Intent

import android.os.Bundle

import android.view.View

import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat

import androidx.recyclerview.widget.LinearLayoutManager

import si.uni.lj.fe.tnuv.homi.databinding.ActivityShoppingListBinding



class ShoppingListActivity : AppCompatActivity() {



    private lateinit var binding: ActivityShoppingListBinding

    private val currentUser = "User1" // Replace with authentication later



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityShoppingListBinding.inflate(layoutInflater)

        setContentView(binding.root)



        val productAdapter = ProductAdapter(

            products = ProductStore.getProducts(),

            onBoughtClick = { /* Placeholder */ },

            onDownvoteClick = { /* Placeholder */ }

        )



        val onBoughtClick: (Product) -> Unit = { product ->

            val updatedProduct = product.copy(isBought = !product.isBought)

            ProductStore.updateProduct(updatedProduct)

            productAdapter.updateProducts(ProductStore.getProducts())

            updateProductVisibility()

            Toast.makeText(

                this@ShoppingListActivity,

                "${product.name} marked as ${if (updatedProduct.isBought) "bought" else "not bought"}",

                Toast.LENGTH_SHORT

            ).show()

        }



        val onDownvoteClick: (Product) -> Unit = { product ->

            val hasDownvoted = product.downvoters.contains(currentUser)

            val updatedDownvoters = if (hasDownvoted) {

                product.downvoters - currentUser // Remove downvote

            } else {

                product.downvoters + currentUser // Add downvote

            }

            val updatedDownvotes = product.downvotes + if (hasDownvoted) -1 else 1

            val updatedProduct = product.copy(

                downvotes = updatedDownvotes,

                downvoters = updatedDownvoters

            )

            ProductStore.updateProduct(updatedProduct)

            productAdapter.updateProducts(ProductStore.getProducts())

            updateProductVisibility()

            Toast.makeText(

                this@ShoppingListActivity,

                if (hasDownvoted) "${product.name} downvote removed ($updatedDownvotes downvotes)"

                else "${product.name} downvoted ($updatedDownvotes downvotes)",

                Toast.LENGTH_SHORT

            ).show()

        }



        productAdapter.setCallbacks(onBoughtClick, onDownvoteClick)



        binding.productRecyclerView.apply {

            layoutManager = LinearLayoutManager(this@ShoppingListActivity)

            adapter = productAdapter

        }



        updateProductVisibility()



        binding.addProductButton.setOnClickListener {

            val name = binding.productInput.text.toString().trim()

            if (name.isEmpty()) {

                Toast.makeText(this, "Please enter a product name", Toast.LENGTH_SHORT).show()

                return@setOnClickListener

            }



            val product = Product(

                id = 0, // Will be set by ProductStore

                name = name,

                timestamp = System.currentTimeMillis(),

                addedBy = currentUser

            )

            ProductStore.addProduct(product)



            productAdapter.updateProducts(ProductStore.getProducts())

            updateProductVisibility()



            binding.productInput.text.clear()

            Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show()

        }



        binding.bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_dashboard -> {

                    startActivity(Intent(this, DashboardActivity::class.java))

                    finish()

                    true

                }

                R.id.nav_calendar -> {

                    startActivity(Intent(this, MainActivity::class.java))

                    finish()

                    true

                }

                R.id.nav_messages -> {

                    startActivity(Intent(this, MessageBoardActivity::class.java))

                    finish()

                    true

                }

                R.id.nav_shopping -> true

                else -> false

            }

        }

        binding.bottomNavigation.selectedItemId = R.id.nav_shopping

    }



    private fun updateProductVisibility() {

        val products = ProductStore.getProducts()

        binding.noProductsText.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE

        binding.productRecyclerView.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE

    }

}