package si.uni.lj.fe.tnuv.homi

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class HomiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Log.d("HomiApplication", "Firebase initialized")
    }
}