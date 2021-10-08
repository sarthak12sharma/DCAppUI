package com.example.dcappui.adapters

import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cast.service.sessions.LaunchSession
import com.example.dcappui.R
import com.example.dcappui.models.DocumentModel
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.SessionManager


class DocumentListAdapter : RecyclerView.Adapter<DocumentListAdapter.DocumentViewHolder>() {

    lateinit var launchSession: LaunchSession
    var documents = mutableListOf<DocumentModel>()
    var clickPosition = 0
    lateinit var recyclerViewClickInterface: RecyclerViewClickInterface
    lateinit var sessionManager: SessionManager


    interface RecyclerViewClickInterface {
        fun onItemClick(position:Int,filePath:String,fileMime:String)
    }


    fun setOnItemClickListener(listener : RecyclerViewClickInterface){
        recyclerViewClickInterface = listener
    }


    fun setDocumentList(documents: List<DocumentModel>) {
        this.documents = documents.toMutableList()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.storage_files_list, parent, false)

        val castContext = CastContext.getSharedInstance(parent.context)
        var state = castContext.castState
        val castFile = view.findViewById<View>(R.id.castFileButton)

        sessionManager = CastContext.getSharedInstance(parent.context).sessionManager
/*        when (state) {
            4 -> castFile.visibility = View.VISIBLE
            1, 2, 3 -> castFile.visibility = View.GONE

        }*/
        return DocumentViewHolder(view,recyclerViewClickInterface)
    }


    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {

        val sizeKbLong : Long = documents[position].size / 1000
        val sizeKbString : String = sizeKbLong.toString() + "Kb"

        val date = documents[position].lastModified

        holder.name.text = documents[position].name
        holder.details.text = sizeKbString
        holder.filePath.text = documents[position].data
        holder.fileMime.text = documents[position].mimeType
        val dateString = DateFormat.format("dd/MM/yyyy HH:mm:ss",date)

        Log.d(" date mod " , " $dateString")
/*            val imageMetaData =
                com.google.android.gms.cast.MediaMetadata(com.google.android.gms.cast.MediaMetadata.MEDIA_TYPE_PHOTO)

            imageMetaData.addImage(WebImage(documents[clickPosition].contentUri))

            val mediaInfo = MediaInfo.Builder(documents[clickPosition].contentUri.toString())
                .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                .setContentType("images/jpg")
                .setMetadata(imageMetaData)
                .build()
            castSession?.remoteMediaClient?.load(
                MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).build()
            )*/

    }

    override fun getItemCount(): Int {
        return documents.size
    }

    class DocumentViewHolder(view: View, listener: RecyclerViewClickInterface) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.documentName)
        val details: TextView = view.findViewById(R.id.documentDetails)
        val castImage: ImageView = view.findViewById(R.id.castFileButton)
        val filePath: TextView = view.findViewById(R.id.filePath)
        val fileMime: TextView = view.findViewById(R.id.fileMime)
        init {
            view.findViewById<ImageView>(R.id.castFileButton).setOnClickListener {
                listener.onItemClick(adapterPosition,filePath.text.toString(),fileMime.text.toString())
            }
        }

    }
}

