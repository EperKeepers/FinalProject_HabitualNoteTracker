package com.espanol.habitualnotetaker

import android.app.Activity
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
import com.espanol.habitualnotetaker.databinding.ActivityAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private var uri: Uri? = null
    private var imageURL: String? = null
    private lateinit var storageReference: StorageReference
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()

        if (DashboardActivity.profilePicture == "" || DashboardActivity.profilePicture == null) {
            Glide.with(this)
                .load(R.drawable.baseline_person_24)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                .into(binding.overlappingImageView)
        } else {
            if(!isDestroyed) {
                Glide.with(this)
                    .load(DashboardActivity.profilePicture)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                    .into(binding.overlappingImageView)
            }
        }

        val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                uri = data?.data!!
                binding.overlappingImageView.setImageURI(uri)
                saveToFirebaseStorage()
            } else {
                Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show()
            }
        }

        binding.overlappingImageView.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_PICK)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }

        binding.profileBackButton.setOnClickListener { finish() }

        binding.profileSignOutButton.setOnClickListener {
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

    }

    private fun saveToFirebaseStorage() {
        val imageReference = storageReference.child("${mAuth.currentUser?.uid.toString()}/Storage/ProfileImage/${mAuth.currentUser?.uid.toString()}-profile_image.jpg")
        val uploadTask = imageReference.putFile(uri!!)
        val uploadingDialog = UploadingDialog()
        uploadingDialog.show(supportFragmentManager, "Loading")
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            uploadingDialog.updateProgress(progress)
        }.addOnSuccessListener { taskSnapshot ->
            val uriTask = taskSnapshot.storage.downloadUrl
            if (uriTask.isComplete) {
                val urlImage = uriTask.result
                imageURL = urlImage.toString()
            } else {
                uriTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val urlImage = task.result
                        imageURL = urlImage.toString()
                        val userUpdateMap = mapOf<String, Any>("profilePicture" to imageURL!!)
                        mDatabase.getReference("user")
                            .child(mAuth.currentUser?.uid.toString())
                            .updateChildren(userUpdateMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile Picture Updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("FirebaseStorageError", "Error getting download URL", task.exception)
                    }
                }
            }
            uploadingDialog.dismiss()
        }.addOnFailureListener {
            uploadingDialog.dismiss()
            Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show()
        }
    }

}