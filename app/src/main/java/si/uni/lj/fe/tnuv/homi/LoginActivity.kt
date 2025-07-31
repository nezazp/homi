package si.uni.lj.fe.tnuv.homi

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.concurrent.Executors

class LoginActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        val signInGoogleButton = findViewById<Button>(R.id.sign_in_w_google_button)
        signInGoogleButton.setOnClickListener {
            launchGoogleSignIn()
        }
        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)

        val signInPasswordButton = findViewById<Button>(R.id.sign_in_w_password)
        signInPasswordButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.username).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()
            signInWithEmailPassword(email, password)
        }

        val registerButton = findViewById<Button>(R.id.register_button)
        registerButton.setOnClickListener {
            showRegistrationDialog()
        }
    }
    private fun signInWithEmailPassword(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d("LoginActivity", "Sign-in task completed, isSuccessful: ${task.isSuccessful}")
                if (task.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread { showError("Email sign-in failed: ${task.exception?.message}") }
                    Log.e("LoginActivity", "Email sign-in failed", task.exception)
                }
            }
    }
    private fun createAccountWithEmailPassword(email: String, password: String) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            runOnUiThread { showError("Please enter a valid email address") }
            Log.e("LoginActivity", "Invalid email format: $email")
            return
        }
        if (password.length < 6) {
            runOnUiThread { showError("Password must be at least 6 characters") }
            Log.e("LoginActivity", "Password too short: $password")
            return
        }

        Log.d("LoginActivity", "Attempting to create account with email: $email")
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d("LoginActivity", "Account creation task completed, isSuccessful: ${task.isSuccessful}")
                if (task.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread { showError("Registration failed: ${task.exception?.message}") }
                    Log.e("LoginActivity", "Registration failed", task.exception)
                }
            }
    }
    private fun showRegistrationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_register, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.register_email)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.register_password)
        val nameEditText = dialogView.findViewById<EditText>(R.id.register_name)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Register New Account")
            .setView(dialogView)
            .setPositiveButton("Register") { _, _ ->
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                val name = nameEditText.text.toString().trim()
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showError("Please enter a valid email address")
                    return@setPositiveButton
                }
                if (password.length < 6) {
                    showError("Password must be at least 6 characters")
                    return@setPositiveButton
                }
                if (name.isEmpty()) {
                    showError("Please enter a name")
                    return@setPositiveButton
                }

                createAccountWithEmailPassword(email, password, name)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun createAccountWithEmailPassword(email: String, password: String, name: String) {
        Log.d("LoginActivity", "Attempting to create account with email: $email, name: $name")
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                Log.d("LoginActivity", "Account creation task completed, isSuccessful: ${task.isSuccessful}")
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, DashboardActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                runOnUiThread { showError("Failed to set name: ${profileTask.exception?.message}") }
                                Log.e("LoginActivity", "Name update failed", profileTask.exception)
                            }
                        }
                } else {
                    runOnUiThread { showError("Registration failed: ${task.exception?.message}") }
                    Log.e("LoginActivity", "Registration failed", task.exception)
                }
            }
    }
    private fun launchGoogleSignIn() {
        val serverClientId = getString(R.string.default_web_client_id)
        if (serverClientId.isEmpty()) {
            runOnUiThread { showError("Server client ID is missing or invalid") }
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val executor = Executors.newSingleThreadExecutor()

        credentialManager.getCredentialAsync(
            context = this,
            request = request,
            cancellationSignal = null,
            executor = executor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    val credential = result.credential
                    if (credential is GoogleIdTokenCredential) {
                        val idToken = credential.idToken
                        if (idToken != null) {
                            signInWithFirebase(idToken)
                        } else {
                            runOnUiThread {
                                showError("Google ID token is null")
                            }
                        }
                    }
                }

                override fun onError(e: GetCredentialException) {
                    runOnUiThread {
                        showError("CredentialManager failed: ${e.message}")
                        Log.e("LoginActivity", "Error type: ${e.javaClass.simpleName}, Message: ${e.message}", e)
                    }
                }
            }
        )
    }

    private fun signInWithFirebase(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task: Task<*> ->
                Log.d("LoginActivity", "Sign-in task completed, isSuccessful: ${task.isSuccessful}")
                if (task.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        // Redirect to DashboardActivity
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                        // Finish the current activity to prevent back navigation
                        finish()
                    }
                } else {
                    runOnUiThread {
                        showError("Firebase sign-in failed")
                    }
                }
            }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}