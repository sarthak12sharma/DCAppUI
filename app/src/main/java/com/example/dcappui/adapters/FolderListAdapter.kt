package com.example.dcappui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dcappui.R

class FolderListAdapter: RecyclerView.Adapter<FolderListAdapter.FoldersViewHolder>() {

    lateinit var folders : List<String>
    lateinit var folderClickInterface: RecyclerViewClickInterface

    interface RecyclerViewClickInterface {
        fun onItemClick(position:Int,folderName: String)
    }

    fun setOnItemClickListener(listener : RecyclerViewClickInterface){
        folderClickInterface = listener
    }

    fun setFoldersList(folders : List<String>){
        this.folders = folders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):FoldersViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.storage_files_list, parent, false)
        return FoldersViewHolder(view,folderClickInterface)
    }

    override fun onBindViewHolder(holder: FolderListAdapter.FoldersViewHolder, position: Int) {

        val folderName = folders[position]
        holder.name.text = folderName
        holder.castImage.visibility = View.GONE
        holder.folderIcon.requestLayout()
        holder.folderIcon.layoutParams.height = 60
        holder.folderIcon.layoutParams.width = 64
        holder.folderIcon.setImageResource(R.drawable.folder_icon)
        holder.details.visibility = View.GONE

    }

    override fun getItemCount(): Int {
        return folders.size
    }

    class FoldersViewHolder(view : View,listener: RecyclerViewClickInterface): RecyclerView.ViewHolder(view) {



        val name: TextView = view.findViewById(R.id.documentName)
        val details: TextView = view.findViewById(R.id.documentDetails)
        val castImage: ImageView = view.findViewById(R.id.castFileButton)
        val folderIcon : ImageView = view.findViewById(R.id.fileIcon)
        init {
            view.setOnClickListener {
                val name = name.text
                listener.onItemClick(adapterPosition, name as String)
            }
        }
}
}