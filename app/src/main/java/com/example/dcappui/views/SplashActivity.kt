package com.example.dcappui.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.dcappui.R

@SuppressLint("CustomSplashScreen")
@Suppress("DEPRECATION")
class SplashActivity: AppCompatActivity() {

    private val TAG = "Splash_Screen"
    var progressBar: ProgressBar?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        },3000)
    }



}