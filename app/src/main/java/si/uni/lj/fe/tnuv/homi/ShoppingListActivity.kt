package si.uni.lj.fe.tnuv.homi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import si.uni.lj.fe.tnuv.homi.databinding.ActivityShoppingListBinding

class ShoppingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShoppingListBinding
    private lateinit var database: com.google.firebase.database.DatabaseReference
    private var groupId: String? = null
    private var currentUser: String = "Guest" // Default until username is fetched
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityShoppingListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = Firebase.database.reference

        // Get current user's username and groupId
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            database.child("users").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    val userData = snapshot.getValue(User::class.java)
                    currentUser = userData?.username ?: "Guest"
                    groupId = userData?.groupId
                    if (groupId == null || groupId!!.isEmpty()) {
                        Toast.makeText(this, "Please join a group to view products", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        setupRecyclerView(uid)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.addProductButton.setOnClickListener {
            if (groupId == null || groupId!!.isEmpty()) {
                Toast.makeText(this, "Please join a group to add products", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = binding.productInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a product name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val product = Product(
                id = "",
                name = name,
                timestamp = System.currentTimeMillis(),
                addedBy = currentUser
            )
            ProductStore.addProduct(database, groupId!!, product) {
                updateProductVisibility()
                binding.productInput.text.clear()
                Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show()
            }
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

    private fun setupRecyclerView(currentUserUid: String) {
        productAdapter = ProductAdapter(
            products = emptyList(),
            onBoughtClick = { product, position ->

                // Update Firebase
                ProductStore.updateProduct(database, groupId!!, product) {
                    // Fetch from Firebase to ensure consistency
                    updateProductVisibility()
                    Toast.makeText(
                        this@ShoppingListActivity,
                        "${product.name} marked as ${if (product.isBought) "bought" else "not bought"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDownvoteClick = { product ->
                val hasDownvoted = product.downvoters.contains(currentUser)
                val updatedDownvoters = if (hasDownvoted) {
                    product.downvoters - currentUser
                } else {
                    product.downvoters + currentUser
                }
                val updatedDownvotes = product.downvotes + if (hasDownvoted) -1 else 1
                val updatedProduct = product.copy(
                    downvotes = updatedDownvotes,
                    downvoters = updatedDownvoters
                )
                ProductStore.updateProduct(database, groupId!!, updatedProduct) {
                    updateProductVisibility()
                    Toast.makeText(
                        this@ShoppingListActivity,
                        if (hasDownvoted) "${product.name} downvote removed ($updatedDownvotes downvotes)"
                        else "${product.name} downvoted ($updatedDownvotes downvotes)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            groupId = groupId!!,
            currentUserUid = currentUserUid
        )

        binding.productRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ShoppingListActivity)
            adapter = productAdapter
        }

        updateProductVisibility()
    }

    private fun updateProductVisibility() {
        ProductStore.getProducts(database, groupId!!) { products ->
            productAdapter.updateProducts(products)
            binding.noProductsText.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            binding.productRecyclerView.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}