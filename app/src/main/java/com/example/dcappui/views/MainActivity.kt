package com.example.dcappui.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cast.discovery.CapabilityFilter
import com.example.cast.discovery.DiscoveryManager
import com.example.cast.service.capability.MediaPlayer
import com.example.dcappui.R
import com.example.dcappui.databinding.ActivityMainBinding
import com.example.dcappui.databinding.GetStartedScreenLayoutBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: GetStartedScreenLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = GetStartedScreenLayoutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        DiscoveryManager.init(applicationContext)



        val imageFilter = CapabilityFilter(MediaPlayer.Display_Image)


        DiscoveryManager.getInstance().setCapabilityFilters(imageFilter)
        DiscoveryManager.getInstance().registerDefaultDeviceTypes()
        DiscoveryManager.getInstance().pairingLevel = DiscoveryManager.PairingLevel.ON
        DiscoveryManager.getInstance().start()

        requestPermission(view)


        binding.startToCastIcon.setOnClickListener(){
            startCastingActivity()
        }

    }

    private fun startCastingActivity() {
        val intent = Intent(this,CastDocumentActivity::class.java)
        startActivity(intent)
    }

    private fun requestPermission(view: View) {

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i("Permission: ", "Granted")
                } else {
                    Log.i("Permission: ", "Denied")
                }
            }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {
                Snackbar.make(view, "Permission Granted", Snackbar.LENGTH_LONG).show()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }



        }

/*        when {ContextCompat.checkSelfPermission(this,Manifest.permission.MANAGE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED -> {
                    Snackbar.make(view,"Manage Permission Granted",Snackbar.LENGTH_LONG).show()
                }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        }*/
    }
}