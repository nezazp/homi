package si.uni.lj.fe.tnuv.homi

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.concurrent.Executors

class LoginActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        val signInButton = findViewById<Button>(R.id.sign_in_button)
        signInButton.setOnClickListener {
            launchGoogleSignIn()
        }
    }

    private fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
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
                        Log.e("LoginActivity", "Error: ", e)
                    }
                }
            }
        )
    }

    private fun signInWithFirebase(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task: Task<*> ->
                if (task.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    }
                    // TODO: go to main screen
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