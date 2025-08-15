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
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.UUID
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
            val email = findViewById<EditText>(R.id.username).text.toString().trim()
            val password = findViewById<EditText>(R.id.password).text.toString().trim()
            signInWithEmailPassword(email, password)
        }

        val registerButton = findViewById<Button>(R.id.register_button)
        registerButton.setOnClickListener {
            showRegistrationDialog()
        }
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            runOnUiThread { showError("Please enter a valid email address") }
            Log.e("LoginActivity", "Invalid email format: $email")
            return
        }
        if (password.isEmpty()) {
            runOnUiThread { showError("Please enter a password") }
            Log.e("LoginActivity", "Password is empty")
            return
        }

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
                    user?.let {
                        // Update display name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        it.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // Save user to Realtime Database
                                    val database = FirebaseDatabase.getInstance()
                                    val usersRef = database.getReference("users")
                                    val userData = mapOf(
                                        "username" to name,
                                        "email" to email,
                                        "groupId" to "" // Initially empty
                                    )
                                    usersRef.child(it.uid).setValue(userData)
                                        .addOnCompleteListener { dbTask ->
                                            if (dbTask.isSuccessful) {
                                                runOnUiThread {
                                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                    showGroupOptionsDialog(it.uid) // Show create/join group dialog
                                                }
                                            } else {
                                                runOnUiThread {
                                                    showError("Failed to save user data: ${dbTask.exception?.message}")
                                                }
                                                Log.e("LoginActivity", "Failed to save user data", dbTask.exception)
                                            }
                                        }
                                } else {
                                    runOnUiThread {
                                        showError("Failed to set name: ${profileTask.exception?.message}")
                                    }
                                    Log.e("LoginActivity", "Name update failed", profileTask.exception)
                                }
                            }
                    }
                } else {
                    runOnUiThread {
                        showError("Registration failed: ${task.exception?.message}")
                    }
                    Log.e("LoginActivity", "Registration failed", task.exception)
                }
            }
    }

    private fun showGroupOptionsDialog(uid: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Group Options")
            .setMessage("Would you like to create a new group or join an existing one?")
            .setPositiveButton("Create Group") { _, _ ->
                showCreateGroupDialog(uid)
            }
            .setNegativeButton("Join Group") { _, _ ->
                showJoinGroupDialog(uid)
            }
            .setNeutralButton("Skip") { dialog, _ ->
                dialog.dismiss()
                // Navigate to DashboardActivity without group
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            }
            .create()
        alertDialog.show()
    }

    private fun showCreateGroupDialog(uid: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val groupNameEditText = dialogView.findViewById<EditText>(R.id.group_name_edit_text)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Create a Group")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val groupName = groupNameEditText.text.toString().trim()
                if (groupName.isEmpty()) {
                    showError("Please enter a group name")
                    return@setPositiveButton
                }
                createGroup(uid, groupName)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showGroupOptionsDialog(uid) // Return to group options
            }
            .create()
        alertDialog.show()
    }

    private fun createGroup(uid: String, groupName: String) {
        val database = FirebaseDatabase.getInstance()
        val groupId = UUID.randomUUID().toString().replace("-", "").substring(0, 8) // 8-character hash
        val groupsRef = database.getReference("groups").child(groupId)
        val usersRef = database.getReference("users").child(uid)

        val groupData = mapOf(
            "name" to groupName,
            "members" to mapOf(uid to true)
        )
        groupsRef.setValue(groupData)
            .addOnCompleteListener { groupTask ->
                if (groupTask.isSuccessful) {
                    usersRef.child("groupId").setValue(groupId)
                        .addOnCompleteListener { userTask ->
                            if (userTask.isSuccessful) {
                                runOnUiThread {
                                    Toast.makeText(this, "Group created! ID: $groupId", Toast.LENGTH_LONG).show()
                                    val intent = Intent(this, DashboardActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                runOnUiThread {
                                    showError("Failed to update user: ${userTask.exception?.message}")
                                }
                                Log.e("LoginActivity", "Failed to update user group", userTask.exception)
                            }
                        }
                } else {
                    runOnUiThread {
                        showError("Failed to create group: ${groupTask.exception?.message}")
                    }
                    Log.e("LoginActivity", "Failed to create group", groupTask.exception)
                }
            }
    }

    private fun showJoinGroupDialog(uid: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_group, null)
        val groupIdEditText = dialogView.findViewById<EditText>(R.id.group_id_edit_text)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Join a Group")
            .setView(dialogView)
            .setPositiveButton("Join") { _, _ ->
                val groupId = groupIdEditText.text.toString().trim()
                if (groupId.isEmpty()) {
                    showError("Please enter a group ID")
                    return@setPositiveButton
                }
                joinGroup(uid, groupId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showGroupOptionsDialog(uid) // Return to group options
            }
            .create()
        alertDialog.show()
    }

    private fun joinGroup(uid: String, groupId: String) {
        val database = FirebaseDatabase.getInstance()
        val groupsRef = database.getReference("groups").child(groupId).child("members")
        val usersRef = database.getReference("users").child(uid)

        // Check if group exists
        groupsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.exists()) {
                // Add user to group members
                groupsRef.child(uid).setValue(true)
                    .addOnCompleteListener { memberTask ->
                        if (memberTask.isSuccessful) {
                            // Update user's groupId
                            usersRef.child("groupId").setValue(groupId)
                                .addOnCompleteListener { userTask ->
                                    if (userTask.isSuccessful) {
                                        runOnUiThread {
                                            Toast.makeText(this, "Joined group $groupId!", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this, DashboardActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else {
                                        runOnUiThread {
                                            showError("Failed to update user group: ${userTask.exception?.message}")
                                        }
                                        Log.e("LoginActivity", "Failed to update user group", userTask.exception)
                                    }
                                }
                        } else {
                            runOnUiThread {
                                showError("Failed to join group: ${memberTask.exception?.message}")
                            }
                            Log.e("LoginActivity", "Failed to join group", memberTask.exception)
                        }
                    }
            } else {
                runOnUiThread {
                    showError("Group $groupId does not exist")
                }
                Log.e("LoginActivity", "Group $groupId not found")
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
                    val user = firebaseAuth.currentUser
                    user?.let {
                        // Save user to Realtime Database
                        val database = FirebaseDatabase.getInstance()
                        val usersRef = database.getReference("users")
                        val userData = mapOf(
                            "username" to (it.displayName ?: "Google User"),
                            "email" to (it.email ?: ""),
                            "groupId" to ""
                        )
                        usersRef.child(it.uid).setValue(userData)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    runOnUiThread {
                                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                        showGroupOptionsDialog(it.uid) // Show create/join group dialog
                                    }
                                } else {
                                    runOnUiThread {
                                        showError("Failed to save user data: ${dbTask.exception?.message}")
                                    }
                                    Log.e("LoginActivity", "Failed to save user data", dbTask.exception)
                                }
                            }
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