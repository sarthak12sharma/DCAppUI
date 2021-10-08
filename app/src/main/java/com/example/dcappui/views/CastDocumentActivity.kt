package com.example.dcappui.views

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.text.format.DateFormat
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.cast.core.MediaInfo
import com.example.cast.device.ConnectableDevice
import com.example.cast.device.ConnectableDeviceListener
import com.example.cast.device.DevicePicker
import com.example.cast.discovery.CapabilityFilter
import com.example.cast.service.DeviceService
import com.example.cast.service.capability.MediaPlayer
import com.example.cast.service.command.ServiceCommandError
import com.example.cast.service.sessions.LaunchSession
import com.example.dcappui.R
import com.example.dcappui.adapters.DocumentListAdapter
import com.example.dcappui.databinding.DocumentCastLayoutBinding
import com.example.dcappui.models.DocumentModel
import com.google.android.material.snackbar.Snackbar
import com.example.cast.core.SubtitleInfo
import com.example.dcappui.adapters.FolderListAdapter
import java.io.File
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet
import kotlin.properties.Delegates


class CastDocumentActivity : AppCompatActivity() {

    private var devices : ConnectableDevice? = null
    private lateinit var devicePicker: DevicePicker

    lateinit var launchSession: LaunchSession
    private lateinit var view: View
    private var pairingAlertDialog: AlertDialog? = null


    private lateinit var binding: DocumentCastLayoutBinding
    private val adapter = DocumentListAdapter()

    private val folderAdapter = FolderListAdapter()

    lateinit var filterDialog : Dialog
    lateinit var sortDialog : Dialog

    lateinit var filesArray : ArrayList<DocumentModel>
    var allFilesBool : Boolean = false
    var filesOnlyBool : Boolean = false
    var foldersOnlyBool : Boolean = false

    var nameBool : Boolean = false
    var sizeBool: Boolean = false
    var lastModBool : Boolean = false

    var folderArray : MutableList<String> = ArrayList()

/*

    private val deviceListener: ConnectableDeviceListener = object : ConnectableDeviceListener {
        override fun onPairingRequired(
            device: ConnectableDevice,
            service: DeviceService,
            pairingType: PairingType
        ) {
            Log.d("2ndScreenAPP", "Connected to " + devices.getIpAddress())
            when (pairingType) {
                PairingType.FIRST_SCREEN -> {
                    Log.d("2ndScreenAPP", "First Screen")
                    pairingAlertDialog?.show()
                }
                PairingType.PIN_CODE, PairingType.MIXED -> {
                    Log.d("2ndScreenAPP", "Pin Code")
                    pairingCodeDialog?.show()
                }
                PairingType.NONE -> {
                }
                else -> {
                }
            }
        }

        override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {
            Log.d("2ndScreenAPP", "onConnectFailed")
            connectFailed(devices)
        }

        override fun onDeviceReady(device: ConnectableDevice) {
            Log.d("2ndScreenAPP", "onPairingSuccess")
            if (pairingAlertDialog?.isShowing() == true) {
                pairingAlertDialog!!.dismiss()
            }
            if (pairingCodeDialog?.isShowing() == true) {
                pairingCodeDialog!!.dismiss()
            }
            registerSuccess(devices)
        }

        override fun onDeviceDisconnected(device: ConnectableDevice) {
            Log.d("2ndScreenAPP", "Device Disconnected")

        }

        override fun onCapabilityUpdated(
            device: ConnectableDevice,
            added: List<String>,
            removed: List<String>
        ) {
        }
    }
*/


    private val mDeviceListener: ConnectableDeviceListener = object : ConnectableDeviceListener {

        override fun onPairingRequired(device: ConnectableDevice, service: DeviceService, pairingType: DeviceService.PairingType
        ) {
            when (pairingType) {
                DeviceService.PairingType.FIRST_SCREEN -> pairingAlertDialog?.show()
                DeviceService.PairingType.MIXED -> pairingAlertDialog?.show()
                else -> {

                }
            }

        }


        override fun onDeviceReady(device: ConnectableDevice) {
            if(pairingAlertDialog?.isShowing == true){
                pairingAlertDialog?.dismiss()
            }
            binding.castButton.setImageResource(R.drawable.ic_cast_black)
            Snackbar.make(view,"Device paired",Snackbar.LENGTH_LONG).show()

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

/*    private val mLaunchListener: MediaPlayer.LaunchListener = object : MediaPlayer.LaunchListener {
        override fun onError(error: ServiceCommandError) {
            Log.d("Connect SDK Sample App", "Could not launch image: $error")
        }

        override fun onSuccess(`object`: MediaPlayer.MediaLaunchObject) {
            Log.d("Connect SDK Sample App", "Successfully launched image!")
            devices?.removeListener(mDeviceListener)
            devices?.disconnect()
            devices = null
        }
    }*/




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

        val folderList = folderArray.toList()

        Log.d(" folder List: " , " $folderList ")

        folderAdapter.setFoldersList(folderList.distinct())
        binding.documentListRV.adapter = folderAdapter

//        DiscoveryManager.init(applicationContext)

//        val imageFilter = CapabilityFilter(MediaPlayer.Display_Image)

/*
        DiscoveryManager.getInstance().setCapabilityFilters(imageFilter)
        DiscoveryManager.getInstance().registerDefaultDeviceTypes()
        DiscoveryManager.getInstance().pairingLevel = DiscoveryManager.PairingLevel.ON
        DiscoveryManager.getInstance().start()*/

        setupPicker()

/*      val fs = StatFs(Environment.getStorageDirectory().path)
        val bytesAvailable : Long
        bytesAvailable = fs.blockSizeLong * fs.availableBlocksLong
        val megAvailable = bytesAvailable / (1024 * 1024 * 1024)
        binding.internalStorageText.text = megAvailable.toString()*/


        val freeBytes = File(getExternalFilesDir(null).toString()).freeSpace
        val free = freeBytes / (1024 * 1024 * 1024)


        val availableBytes = File(getExternalFilesDir(null).toString()).totalSpace
        val available = availableBytes / (1024 * 1024 * 1024)


        binding.internalStorageText.text = free.toString() + " GB" + " / " + available.toString() + " GB"




        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAdd : String = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)

        Log.d("ip address","ip: $ipAdd")


       // adapter.setDocumentList(files)
       // binding.documentListRV.adapter = adapter



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
                allFilesBool-> {
                    allCheckBox.isChecked = true
                }
                filesOnlyBool -> {
                    filesOnlyCheckbox.isChecked = true
                }
                foldersOnlyBool-> {
                    foldersOnlyCheckbox.isChecked = true
                }
            }

            allCheckBox.setOnClickListener {
               // adapter.setDocumentList(files)
               // binding.documentListRV.adapter = adapter
                filesOnlyCheckbox.isChecked = false
                foldersOnlyCheckbox.isChecked = false
                allFilesBool = true
                filesOnlyBool = false
                foldersOnlyBool = false
            }

            filesOnlyCheckbox.setOnClickListener {
                //adapter.setDocumentList(files)
                //binding.documentListRV.adapter = adapter
                allCheckBox.isChecked = false
                foldersOnlyCheckbox.isChecked = false
                filesOnlyBool = true
                allFilesBool = false
                foldersOnlyBool = false
            }


            foldersOnlyCheckbox.setOnClickListener {
                //adapter.setDocumentList(files)
                //binding.documentListRV.adapter = adapter
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

            when{
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
                filesArray = ArrayList(files)
                filesArray.sortBy {
                    it.name
                }
             //   adapter.setDocumentList(filesArray)
              //  binding.documentListRV.adapter = adapter
                sizeCheckBox.isChecked = false
                lastModCheckbox.isChecked = false

                nameBool = true
                sizeBool = false
                lastModBool = false
            }



            sizeCheckBox.setOnClickListener{
                filesArray = ArrayList(files)
                filesArray.sortBy {
                    it.size
                }
            //    adapter.setDocumentList(filesArray)
              //  binding.documentListRV.adapter = adapter

                nameCheckBox.isChecked = false
                lastModCheckbox.isChecked = false

                sizeBool = true
                nameBool = false
                lastModBool = false
            }

            lastModCheckbox.setOnClickListener {

                filesArray = ArrayList(files)
                filesArray.sortBy {
                    it.lastModified
                }

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





        adapter.setOnItemClickListener(object : DocumentListAdapter.RecyclerViewClickInterface{

            override fun onItemClick(position: Int) {


                val iconURL = "http://www.connectsdk.com/files/2013/9656/8845/test_image_icon.jpg"


                val mimeType: String = files[position].mimeType

                val iconUrl: String = R.drawable.ic_favorites.toString()

                val fileDataPath : String = files[position].data

                val newPath = fileDataPath.substring(1)

                val encodedPath = URLEncoder.encode(newPath,"utf-8")

                val absolutePath = "http://$ipAdd:9000/$encodedPath"


                Log.d(" absolute path ", "$absolutePath ")

                if(mimeType == "image/jpeg" || mimeType == "image/png" ){
                    displayImage(absolutePath, mimeType, iconURL)
                }else if (mimeType == "video/mp4"){
                    playVideo(absolutePath, mimeType, iconURL)
                }



            }

        })



    }


        private fun playVideo(absolutePath:  String, mimeType : String, iconUrl: String) {
        var shouldLoop : Boolean = true
        var subtitleInfo : SubtitleInfo.Builder? = null
        val mediaInfo: MediaInfo? = MediaInfo.Builder(absolutePath,mimeType)
            .setTitle("Video")
            .setDescription("Video description")
            .setIcon(iconUrl)
            .build()

        val mediaPlayer: MediaPlayer? = devices?.getCapability(MediaPlayer::class.java)
        mediaPlayer?.playMedia(mediaInfo,shouldLoop,object : MediaPlayer.LaunchListener{
            override fun onError(error: ServiceCommandError?) {
                Log.e("Error", "Error playing video", error)
            }
            override fun onSuccess(`object`: MediaPlayer.MediaLaunchObject?) {
                if (`object` != null) {
                    launchSession = `object`.launchSession
                }
                Log.d("Media", "Started casting media")
                Snackbar.make(view,"Started casting media",Snackbar.LENGTH_LONG).show()
            }

        })
    }

    private fun displayImage(absolutePath:  String, mimeType : String, iconUrl: String) {
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
                    Snackbar.make(view,"Started casting media",Snackbar.LENGTH_LONG).show()
                }
            })
    }


    private fun setupPicker() {
        devicePicker = DevicePicker(this)
        binding.castButton.setOnClickListener(){
            val pickerClickListener = OnItemClickListener{arg0, arg1, arg2, arg3 ->
                devices = arg0.getItemAtPosition(arg2) as ConnectableDevice
                devices!!.addListener(mDeviceListener)
                devices!!.connect()
            }
            val dialog : AlertDialog = devicePicker.getPickerDialog("Select a device",pickerClickListener)
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
            MediaStore.Files.FileColumns.PARENT,
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
            val parentName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.PARENT)
            //val nameParent = cursor.getColumnName(idColumn)
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
                val parent = cursor.getString(parentName)
                if (name != null && mime != null) {
                    Log.d("file names", "file:$name")
                    Log.d("date tag","data: $dateMod")
                    folders.add(DocumentModel(id, mime, name, dateMod, size, contentUri,data,parent))
                    val folderName = data
                    val delim = "/0/"
                    val folderArr = folderName.split(delim).toTypedArray()

                    val secondPart = folderArr[1]
                    val delim2 = "/"
                    val folderNameArr = secondPart.split(delim2).toTypedArray()
                    val folderNameLog : String = folderNameArr[0]

                    Log.d(" folder Name split: ", " $folderNameLog ")

                    folderArray.add(folderNameArr[0])

                }
            }
            folders.toList()

        }
        return folders.toList()
    }
}


/*
        val docString: String = when (DocumentReaderUtil.getMimeType(fileUri, applicationContext)) {
            "text/plain" -> DocumentReaderUtil.readTxtFromUri(fileUri, applicationContext)
            "application/pdf" -> DocumentReaderUtil.readPdfFromUri(fileUri, applicationContext)
            "application/msword" -> DocumentReaderUtil.readWordDocFromUri(
                fileUri,
                applicationContext
            )
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                DocumentReaderUtil.readWordDocFromUri(fileUri, applicationContext)
            else -> ""
        }
        Log.d("pdf file", docString)
*/


/* if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
            val sm = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            intent = sm.primaryStorageVolume.createOpenDocumentTreeIntent()

            val startDir = "Documents"
            var uriRoot = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI")
            var scheme = uriRoot.toString()
            Log.d("Debug","INITIAL_URI scheme: $scheme")
            scheme = scheme.replace("/root/","/documents/")
            scheme += "%3A$startDir"
            uriRoot = Uri.parse(scheme)
            intent.putExtra("android.provider.extra.INITIAL_URI", uriRoot)
            Log.d("Debug","uri: $uriRoot")

            startActivityForResult(intent,REQUEST_CODE)
        selectDocuments()

   }

   private fun selectDocuments() {
       val uri = MediaStore.Files.getContentUri("external")
       var selection = MediaStore.getDocumentUri(this,uri)

       var rs = contentResolver.query(uri,null,selection,null,null)
   }

    private fun selectDocuments() {
        val browseDocs = Intent(Intent.ACTION_GET_CONTENT)
        browseDocs.type = "application/documents"
        browseDocs.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(browseDocs,"Select docs"),REQUEST_CODE)
    }*/

//     val file = File(Environment.getExternalStorageDirectory().absoluteFile,".pdf")
//     Log.d("root directories",file.toString())


//   val fileUri = MediaStore.Files.getContentUri("external")


//   val file = File(Environment.getRootDirectory(),".")

//  val uriFile = Uri.fromFile(file)

//val doc : String = DocumentReaderUtil.readPdfFromFile(file, applicationContext)

// Log.d("document string:",doc)

//   getFilePath(this,fileUri)

/* private fun getFilePath(context: Context, uri: Uri?): String? {
     var cursor: Cursor? = null
     val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)
     try {
         if (uri == null) return null
        contentResolver.query(uri, projection, null, null,
             null)?.use { cursor ->
             val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
             while (cursor.moveToNext()) {
                 val name = cursor.getString(index)
                 if(name!=null){
                    // Log.d("folders","folder:"+  name)
                 }
//                folders.add(FoldersModel(id, name, dateMod, size, contentUri))

             }
         }
         *//*if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(index)
            }*//*


        } finally {
            cursor?.close()
        }
        return null
    }*/

