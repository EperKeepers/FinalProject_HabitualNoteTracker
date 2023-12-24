package com.espanol.habitualnotetaker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.espanol.habitualnotetaker.databinding.ActivityAddNoteBinding
import com.espanol.habitualnotetaker.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private var uri: Uri? = null
    private var imageURL: String? = null
    private lateinit var storageReference: StorageReference
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mAuth: FirebaseAuth

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val calendar = Calendar.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()

        binding.textViewDate.text = SimpleDateFormat("yyyy-MM-dd").format(calendar.time).toString()
        binding.textViewTime.text = SimpleDateFormat("hh:mm a").format(calendar.time).toString()

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

        binding.addNoteSaveTextView.setOnClickListener {
            val uploadingDialog = UploadingDialog()
            uploadingDialog.show(supportFragmentManager, "Loading")
            saveToFirebaseStorage(uploadingDialog)
        }

        binding.addNoteBackButton.setOnClickListener { finish() }
    }

    private fun saveToFirebaseStorage(uploadingDialog: UploadingDialog) {
        val noteKey = mDatabase.getReference("notes").push().key
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
                mDatabase.getReference("notes").child(noteKey!!).setValue(
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
                    Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    uploadingDialog.dismiss()
                    Toast.makeText(this, "Note failed to upload", Toast.LENGTH_SHORT).show()
                }
            } else {
                uriTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val urlImage = task.result
                        imageURL = urlImage.toString()
                        mDatabase.getReference("notes").child(noteKey!!).setValue(
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
                            Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            uploadingDialog.dismiss()
                            Toast.makeText(this, "Note failed to upload", Toast.LENGTH_SHORT).show()
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