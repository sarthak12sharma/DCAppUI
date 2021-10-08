package com.example.dcappui.views

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cast.core.MediaInfo
import com.example.cast.core.SubtitleInfo
import com.example.cast.device.ConnectableDevice
import com.example.cast.device.ConnectableDeviceListener
import com.example.cast.device.DevicePicker
import com.example.cast.service.DeviceService
import com.example.cast.service.capability.MediaPlayer
import com.example.cast.service.command.ServiceCommandError
import com.example.cast.service.sessions.LaunchSession
import com.example.dcappui.R
import com.example.dcappui.adapters.DocumentListAdapter
import com.example.dcappui.adapters.FolderListAdapter
import com.example.dcappui.databinding.DocumentCastLayoutBinding
import com.example.dcappui.models.DocumentModel
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.net.URLEncoder


class CastDocumentActivity : AppCompatActivity() {

    private var devices: ConnectableDevice? = null
    private lateinit var devicePicker: DevicePicker

    lateinit var launchSession: LaunchSession
    private lateinit var view: View
    private var pairingAlertDialog: AlertDialog? = null


    private lateinit var binding: DocumentCastLayoutBinding
    private val adapter = DocumentListAdapter()

    private val folderAdapter = FolderListAdapter()

    lateinit var filterDialog: Dialog
    lateinit var sortDialog: Dialog

    var allFilesBool: Boolean = false
    var filesOnlyBool: Boolean = false
    var foldersOnlyBool: Boolean = false

    var nameBool: Boolean = false
    var sizeBool: Boolean = false
    var lastModBool: Boolean = false

    var folderArray: MutableList<String> = ArrayList()
    var folderSpecificFiles: MutableList<DocumentModel> = ArrayList()


    private val mDeviceListener: ConnectableDeviceListener = object : ConnectableDeviceListener {

        override fun onPairingRequired(
            device: ConnectableDevice,
            service: DeviceService,
            pairingType: DeviceService.PairingType
        ) {
            when (pairingType) {
                DeviceService.PairingType.FIRST_SCREEN -> pairingAlertDialog?.show()
                DeviceService.PairingType.MIXED -> pairingAlertDialog?.show()
                else -> {

                }
            }

        }


        override fun onDeviceReady(device: ConnectableDevice) {
            if (pairingAlertDialog?.isShowing == true) {
                pairingAlertDialog?.dismiss()
            }
            binding.castButton.setImageResource(R.drawable.ic_cast_black)
            Snackbar.make(view, "Device paired", Snackbar.LENGTH_LONG).show()

        }

        override fun onDeviceDisconnected(device: ConnectableDevice) {
            binding.connectedDeviceName.visibility = View.GONE
            binding.castButton.visibility = View.VISIBLE
        }

        override fun onConnectionFailed(
            device: ConnectableDevice,
            error: ServiceCommandError
        ) {
            devices?.removeListener(this)
            devices = null
        }

        override fun onCapabilityUpdated(
            device: ConnectableDevice,
            added: List<String>, removed: List<String>
        ) {
            // TODO Auto-generated method stub
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DocumentCastLayoutBinding.inflate(layoutInflater)
        filterDialog = Dialog(this)
        sortDialog = Dialog(this)

        view = binding.root
        filterDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        sortDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(view)
        val files = loadFolders()

        for (item in files) {
            val directoryName = item.bucketName
            folderArray.add(directoryName)
        }

        Log.d(" folder List: ", " $folderArray ")

        folderAdapter.setFoldersList(folderArray.distinct())
        binding.documentListRV.adapter = folderAdapter


        setupPicker()


        val freeBytes = File(getExternalFilesDir(null).toString()).freeSpace
        val free = freeBytes / (1024 * 1024 * 1024)


        val availableBytes = File(getExternalFilesDir(null).toString()).totalSpace
        val available = availableBytes / (1024 * 1024 * 1024)


        binding.internalStorageText.text =
            free.toString() + " GB" + " / " + available.toString() + " GB"


        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAdd: String = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)

        Log.d("ip address", "ip: $ipAdd")




        binding.filterIcon.setOnClickListener {

            filterDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            filterDialog.setContentView(R.layout.filter_dialog)
            filterDialog.setCancelable(true)
            filterDialog.show()

            val allCheckBox = filterDialog.findViewById<CheckBox>(R.id.allCheckBox)
            val filesOnlyCheckbox = filterDialog.findViewById<CheckBox>(R.id.filesOnlyCheckBox)
            val foldersOnlyCheckbox = filterDialog.findViewById<CheckBox>(R.id.foldersOnlyCheckBox)
            val filterOkButton = filterDialog.findViewById<TextView>(R.id.filterOkButton)

            when {
                allFilesBool -> {
                    allCheckBox.isChecked = true
                }
                filesOnlyBool -> {
                    filesOnlyCheckbox.isChecked = true
                }
                foldersOnlyBool -> {
                    foldersOnlyCheckbox.isChecked = true
                }
            }

            allCheckBox.setOnClickListener {

                folderAdapter.setFoldersList(folderArray.distinct())
                Log.d(" all array: ", " $folderArray")
                binding.documentListRV.adapter = folderAdapter
                filesOnlyCheckbox.isChecked = false
                foldersOnlyCheckbox.isChecked = false
                allFilesBool = true
                filesOnlyBool = false
                foldersOnlyBool = false
            }

            filesOnlyCheckbox.setOnClickListener {
                adapter.setDocumentList(files)
                binding.documentListRV.adapter = adapter
                allCheckBox.isChecked = false
                foldersOnlyCheckbox.isChecked = false
                filesOnlyBool = true
                allFilesBool = false
                foldersOnlyBool = false
            }


            foldersOnlyCheckbox.setOnClickListener {
                folderAdapter.setFoldersList(folderArray.distinct())
                Log.d(" Folder only array: ", " $folderArray")
                binding.documentListRV.adapter = folderAdapter
                filesOnlyCheckbox.isChecked = false
                allCheckBox.isChecked = false
                foldersOnlyBool = true
                filesOnlyBool = false
                allFilesBool = false
            }



            filterOkButton?.setOnClickListener {
                filterDialog.dismiss()
            }


        }







        binding.sortIcon.setOnClickListener {

            sortDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            sortDialog.setCancelable(false)
            sortDialog.setContentView(R.layout.sort_dialog)
            sortDialog.setCancelable(true)
            sortDialog.show()

            val nameCheckBox = sortDialog.findViewById<CheckBox>(R.id.nameCheckBox)
            val sizeCheckBox = sortDialog.findViewById<CheckBox>(R.id.sizeCheckBox)
            val lastModCheckbox = sortDialog.findViewById<CheckBox>(R.id.lastModifiedCheckBox)
            val okButton = sortDialog.findViewById<TextView>(R.id.sortOkButton)

            when {
                nameBool -> {
                    nameCheckBox.isChecked = true
                }
                sizeBool -> {
                    sizeCheckBox.isChecked = true
                }
                lastModBool -> {
                    lastModCheckbox.isChecked = true
                }
            }

            nameCheckBox?.setOnClickListener {
                folderSpecificFiles.sortBy {
                    it.name
                }
                adapter.setDocumentList(folderSpecificFiles)
                binding.documentListRV.adapter = adapter
                sizeCheckBox.isChecked = false
                lastModCheckbox.isChecked = false

                nameBool = true
                sizeBool = false
                lastModBool = false
            }



            sizeCheckBox.setOnClickListener {
                folderSpecificFiles.sortBy {
                    it.size
                }
                adapter.setDocumentList(folderSpecificFiles)
                binding.documentListRV.adapter = adapter

                nameCheckBox.isChecked = false
                lastModCheckbox.isChecked = false

                sizeBool = true
                nameBool = false
                lastModBool = false
            }

            lastModCheckbox.setOnClickListener {

                folderSpecificFiles.sortBy {
                    it.lastModified
                }
                adapter.setDocumentList(folderSpecificFiles)
                binding.documentListRV.adapter = adapter

                nameCheckBox.isChecked = false
                sizeCheckBox.isChecked = false

                lastModBool = true
                nameBool = false
                sizeBool = false
            }

            okButton.setOnClickListener {
                sortDialog.dismiss()
            }

        }



        adapter.setOnItemClickListener(object : DocumentListAdapter.RecyclerViewClickInterface {
            override fun onItemClick(position: Int, filePath: String, fileMime: String) {
                val iconURL = "http://www.connectsdk.com/files/2013/9656/8845/test_image_icon.jpg"
                Log.d(" click position: ", " $position")
                Log.d(" file path: ", " $filePath")
                val newPath = filePath.substring(1)
                val encodedPath = URLEncoder.encode(newPath, "utf-8")
                val absolutePath = "http://$ipAdd:9000/$encodedPath"
                Log.d(" absolute path: ", " $absolutePath")
                if (fileMime == "image/jpeg" || fileMime == "image/png") {
                    displayImage(absolutePath, fileMime, iconURL)
                } else if (fileMime == "video/mp4") {
                    playVideo(absolutePath, fileMime, iconURL)
                }

            }

        })

        folderAdapter.setOnItemClickListener(object : FolderListAdapter.RecyclerViewClickInterface {
            override fun onItemClick(position: Int, folderName: String) {
                Log.d(" position:", " $position ")

                for (item in files) {
                    Log.d(" for loop ", " $item")
                    if (folderName == item.bucketName) {
                        folderSpecificFiles.add(item)
                        Log.d(" name: ", " $folderSpecificFiles")

                    }
                }
                adapter.setDocumentList(folderSpecificFiles)
                binding.documentListRV.adapter = adapter
            }

        })

    }


    private fun playVideo(absolutePath: String, mimeType: String, iconUrl: String) {
        var shouldLoop: Boolean = true
        var subtitleInfo: SubtitleInfo.Builder? = null
        val mediaInfo: MediaInfo? = MediaInfo.Builder(absolutePath, mimeType)
            .setTitle("Video")
            .setDescription("Video description")
            .setIcon(iconUrl)
            .build()

        val mediaPlayer: MediaPlayer? = devices?.getCapability(MediaPlayer::class.java)
        mediaPlayer?.playMedia(mediaInfo, shouldLoop, object : MediaPlayer.LaunchListener {
            override fun onError(error: ServiceCommandError?) {
                Log.e("Error", "Error playing video", error)
            }

            override fun onSuccess(`object`: MediaPlayer.MediaLaunchObject?) {
                if (`object` != null) {
                    launchSession = `object`.launchSession
                }
                Log.d("Media", "Started casting media")
                Snackbar.make(view, "Started casting media", Snackbar.LENGTH_LONG).show()
            }

        })
    }

    private fun displayImage(absolutePath: String, mimeType: String, iconUrl: String) {
        val mediaInfo: MediaInfo? = MediaInfo.Builder(absolutePath, mimeType)
            .setTitle("title")
            .setDescription("description")
            .setIcon(iconUrl)
            .build()

        val mediaPlayer: MediaPlayer? = devices?.getCapability(MediaPlayer::class.java)
        mediaPlayer?.displayImage(
            mediaInfo,
            object : MediaPlayer.LaunchListener {
                override fun onError(error: ServiceCommandError?) {
                    Log.e("Error", "Error playing video", error)
                }

                override fun onSuccess(`object`: MediaPlayer.MediaLaunchObject?) {
                    if (`object` != null) {
                        launchSession = `object`.launchSession
                    }
                    Log.d("Media", "Started casting media")
                    Snackbar.make(view, "Started casting media", Snackbar.LENGTH_LONG).show()
                }
            })
    }


    private fun setupPicker() {
        devicePicker = DevicePicker(this)
        binding.castButton.setOnClickListener() {
            val pickerClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
                devices = arg0.getItemAtPosition(arg2) as ConnectableDevice
                devices!!.addListener(mDeviceListener)
                devices!!.connect()
            }
            val dialog: AlertDialog =
                devicePicker.getPickerDialog("Select a device", pickerClickListener)
            dialog.show()
        }
    }


//        setupPicker()

/*        discoveryManager = DiscoveryManager.getInstance()
        discoveryManager.registerDefaultDeviceTypes()
        discoveryManager.pairingLevel = DiscoveryManager.PairingLevel.ON*/

//        DiscoveryManager.getInstance().start()

//        loadFolders()


/*        var mMediaRouteButton = binding.mediaRouteButton
        CastButtonFactory.setUpMediaRouteButton(this,mMediaRouteButton)*/


/*    private fun setupPicker() {
        var dp = DevicePicker(this)
        var dialog = dp.getPickerDialog(
            "Device List"
        ) { arg0, arg1, arg2, arg3 ->
            devices = arg0.getItemAtPosition(arg2) as ConnectableDevice
            devices.addListener(deviceListener)
            devices.setPairingType(null)
            devices.connect()
            dp.pickDevice(devices)
        }
        dialog.show()

    }
    fun connectFailed(device: ConnectableDevice?) {
        if (device != null) Log.d("2ndScreenAPP", "Failed to connect to " + device.ipAddress)
        devices.removeListener(deviceListener)
        devices.disconnect()
    }

    fun registerSuccess(device: ConnectableDevice?) {
        Log.d("2ndScreenAPP", "successful register")
    }*/


    private fun loadFolders(): List<DocumentModel> {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
        )

        val folders = mutableListOf<DocumentModel>()
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateModColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val fileData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val bucketIdM = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketName =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateMod = cursor.getLong(dateModColumn)
                val size = cursor.getLong(sizeColumn)
                val mime = cursor.getString(mimeType)
                val data = cursor.getString(fileData)
                val contentUri = ContentUris.withAppendedId(
                    uri, id
                )
                val bucketId = cursor.getString(bucketIdM)
                val bucketDisplayName = cursor.getString(bucketName)
                if (name != null && mime != null) {
                    Log.d("file names", "file:$name")
                    Log.d("date tag", "data: $dateMod")
                    Log.d(" bucket id: ", " id:$bucketId ")
                    Log.d(" bucket display name: ", " id:$bucketDisplayName ")
                    folders.add(
                        DocumentModel(
                            id,
                            mime,
                            name,
                            dateMod,
                            size,
                            contentUri,
                            data,
                            bucketId,
                            bucketDisplayName
                        )
                    )


                }
            }
            folders.toList()

        }
        return folders.toList()
    }
}



