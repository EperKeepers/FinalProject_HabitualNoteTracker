package com.espanol.habitualnotetaker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.espanol.habitualnotetaker.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        binding.registerButton.setOnClickListener {
            if (binding.registerNameEditText.text.isBlank()) {
                Toast.makeText(this, "Name must not be blank", Toast.LENGTH_SHORT).show()
            } else {
                if (binding.registerEmailEditText.text.isBlank()) {
                    Toast.makeText(this, "Email must not be blank", Toast.LENGTH_SHORT).show()
                } else {
                    if (!Patterns.EMAIL_ADDRESS.matcher(binding.registerEmailEditText.text).matches()) {
                        Toast.makeText(this, "Email must be valid", Toast.LENGTH_SHORT).show()
                    } else {
                        if (binding.registerPasswordEditText.text.isBlank()) {
                            Toast.makeText(this, "Password must not be blank", Toast.LENGTH_SHORT).show()
                        } else {
                            if (binding.registerPasswordEditText.text.toString().length < 6) {
                                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                            } else {
                                if (binding.registerPasswordEditText.text.toString() == binding.registerConfirmPasswordEditText.text.toString()) {
                                    mAuth.createUserWithEmailAndPassword(
                                        binding.registerEmailEditText.text.toString(),
                                        binding.registerPasswordEditText.text.toString()
                                    ).addOnSuccessListener {
                                        mDatabase.getReference("user").child(mAuth.currentUser?.uid.toString()).setValue(
                                            UserData(
                                                mAuth.currentUser?.uid.toString(),
                                                binding.registerNameEditText.text.toString(),
                                                binding.registerEmailEditText.text.toString(),
                                                ""
                                            )
                                        ).addOnSuccessListener {
                                            Toast.makeText(this, "Register success", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, DashboardActivity::class.java))
                                            finish()
                                        }.addOnFailureListener {
                                            Toast.makeText(this, "User data failed to save", Toast.LENGTH_SHORT).show()
                                        }
                                    }.addOnFailureListener {
                                        Toast.makeText(this, "Register failed", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Password must match", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.loginRegisterTextView.setOnClickListener {
            finish()
        }

    }
}