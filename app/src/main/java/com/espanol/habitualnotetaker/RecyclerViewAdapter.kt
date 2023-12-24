package com.espanol.habitualnotetaker

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.espanol.habitualnotetaker.databinding.RecyclerViewCustomLayoutNotesBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class RecyclerViewAdapter(private val originalList: List<NoteData>, private val activity: Activity) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>(), Filterable {

    private var filteredList: List<NoteData> = originalList


    class ViewHolder(val binding: RecyclerViewCustomLayoutNotesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RecyclerViewCustomLayoutNotesBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = filteredList[position]
        if (currentItem.image == "" || currentItem.image == null) {
            Glide.with(activity)
                .load(R.drawable.baseline_image_24)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                .into(holder.binding.noteHeaderImageView)
        } else {
            Glide.with(activity)
                .load(currentItem.image)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                .into(holder.binding.noteHeaderImageView)
        }
        holder.binding.noteHeaderTitleTextView.text = currentItem.title
        holder.binding.noteSubContentTextView.text = currentItem.content
        holder.binding.dateTextView.text = currentItem.date
        holder.binding.timeTextView.text = currentItem.time
        holder.binding.categoriesTextView.text = currentItem.category
        holder.binding.editImageButton.setOnClickListener {
            val intent = Intent(activity, EditNoteActivity::class.java)
            intent.putExtra("noteUID", currentItem.noteUID)
            activity.startActivity(intent)
        }
        holder.binding.deleteImageButton.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete the note '${currentItem.title}'?")
                .setPositiveButton("Delete") { _, _ ->
                    val mDatabase = FirebaseDatabase.getInstance()
                    val historyKey = mDatabase.getReference("history").push().key
                    mDatabase.getReference("history").child(historyKey!!).setValue(
                        NoteData(
                            historyKey,
                            currentItem.title,
                            currentItem.content,
                            currentItem.date,
                            currentItem.category,
                            currentItem.category,
                            currentItem.time,
                            ""
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(activity, "History added", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(activity, "History failed to upload", Toast.LENGTH_SHORT).show()
                    }
                    mDatabase.getReference("notes").orderByChild("noteUID").equalTo(currentItem.noteUID)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (data in snapshot.children) {
                                    val noteKey = data.key
                                    if (noteKey != null) {
                                        mDatabase.getReference("notes").child(noteKey).removeValue()
                                            .addOnSuccessListener {
                                                Toast.makeText(activity, "Note deleted", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(activity, "Failed to delete note", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("FirebaseError", "Error: ${error.message}")
                            }
                        })
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint.isNullOrBlank()) {
                    results.values = originalList
                } else {
                    val query = constraint.toString().lowercase(Locale.ROOT).trim()
                    val filtered = originalList.filter { noteData ->
                        noteData.title?.lowercase(Locale.ROOT)?.contains(query) == true ||
                                noteData.content?.lowercase(Locale.ROOT)?.contains(query) == true ||
                                noteData.category?.lowercase(Locale.ROOT)?.contains(query) == true
                    }
                    results.values = filtered
                }
                return results
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as List<NoteData>
                notifyDataSetChanged()
            }
        }
    }

}