package com.espanol.habitualnotetaker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.espanol.habitualnotetaker.databinding.ActivityEditNoteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditNoteBinding
    private var uri: Uri? = null
    private var imageURL: String? = null
    private lateinit var storageReference: StorageReference
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()

        val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                uri = data?.data!!
                binding.backgroundImageView.setImageURI(uri)
            } else {
                Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show()
            }
        }

        binding.addPhotoIcon.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_PICK)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }

        mDatabase.getReference("notes").orderByChild("noteUID").equalTo(intent.getStringExtra("noteUID")).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { data ->
                    val noteData = data.getValue(NoteData::class.java)

                    binding.addNoteTitleEditText.setText(noteData?.title)
                    binding.addNoteStartNoteEditText.setText(noteData?.content)
                    binding.textViewDate.text = noteData?.date
                    binding.textViewTime.text = noteData?.time

                    if (noteData?.image == "" || noteData?.image == null) {
                        Glide.with(this@EditNoteActivity)
                            .load(R.drawable.baseline_person_24)
                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                            .into(binding.backgroundImageView)
                    } else {
                        if (!isDestroyed) {
                            Glide.with(this@EditNoteActivity)
                                .load(noteData.image)
                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                                .into(binding.backgroundImageView)
                        }
                    }

                    binding.addNoteSaveTextView.setOnClickListener {
                        val uploadingDialog = UploadingDialog()
                        uploadingDialog.show(supportFragmentManager, "Loading")
                        noteData?.noteUID?.let { saveToFirebaseStorage(uploadingDialog, it) }
                    }

                    binding.deleteIcon.setOnClickListener {
                        val builder = AlertDialog.Builder(this@EditNoteActivity)
                        builder.setTitle("Confirm Delete")
                            .setMessage("Are you sure you want to delete the note '${title}'?")
                            .setPositiveButton("Delete") { _, _ ->
                                val mDatabase = FirebaseDatabase.getInstance()
                                val historyKey = mDatabase.getReference("history").push().key
                                mDatabase.getReference("history").child(historyKey!!).setValue(
                                    NoteData(
                                        historyKey,
                                        noteData?.title.toString(),
                                        noteData?.content.toString(),
                                        noteData?.category.toString(),
                                        noteData?.image.toString(),
                                        noteData?.date.toString(),
                                        noteData?.time.toString(),
                                        ""
                                    )
                                ).addOnSuccessListener {
                                    Toast.makeText(this@EditNoteActivity, "History added", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener {
                                    Toast.makeText(this@EditNoteActivity, "History failed to upload", Toast.LENGTH_SHORT).show()
                                }
                                mDatabase.getReference("notes").orderByChild("noteUID").equalTo(noteData!!.noteUID)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            for (i in snapshot.children) {
                                                val noteKey = i.key
                                                if (noteKey != null) {
                                                    mDatabase.getReference("notes").child(noteKey).removeValue()
                                                        .addOnSuccessListener {
                                                            Toast.makeText(this@EditNoteActivity, "Note deleted", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(this@EditNoteActivity, "Failed to delete note", Toast.LENGTH_SHORT).show()
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
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })

        binding.addNoteBackButton.setOnClickListener { finish() }
    }

    private fun saveToFirebaseStorage(uploadingDialog: UploadingDialog, noteKey: String) {
        val imageReference = storageReference.child("Storage/noteImage/$noteKey-note_image.jpg")

        val uploadTask = imageReference.putFile(uri!!)
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            uploadingDialog.updateProgress(progress)
        }.addOnSuccessListener { taskSnapshot ->
            val uriTask = taskSnapshot.storage.downloadUrl
            if (uriTask.isComplete) {
                val urlImage = uriTask.result
                imageURL = urlImage.toString()

                mDatabase.getReference("notes").child(noteKey).setValue(
                    NoteData(
                        noteKey,
                        binding.addNoteTitleEditText.text.toString(),
                        binding.addNoteStartNoteEditText.text.toString(),
                        binding.categorySpinner.selectedItem.toString(),
                        imageURL,
                        binding.textViewDate.text.toString(),
                        binding.textViewTime.text.toString(),
                        ""
                    )
                ).addOnSuccessListener {
                    uploadingDialog.dismiss()
                    Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    uploadingDialog.dismiss()
                    Toast.makeText(this, "Failed to update note", Toast.LENGTH_SHORT).show()
                }
            } else {
                uriTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val urlImage = task.result
                        imageURL = urlImage.toString()

                        mDatabase.getReference("notes").child(noteKey).setValue(
                            NoteData(
                                noteKey,
                                binding.addNoteTitleEditText.text.toString(),
                                binding.addNoteStartNoteEditText.text.toString(),
                                binding.categorySpinner.selectedItem.toString(),
                                imageURL,
                                binding.textViewDate.text.toString(),
                                binding.textViewTime.text.toString(),
                                ""
                            )
                        ).addOnSuccessListener {
                            uploadingDialog.dismiss()
                            Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            uploadingDialog.dismiss()
                            Toast.makeText(this, "Failed to update note", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("FirebaseStorageError", "Error getting download URL", task.exception)
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show()
        }
    }

}