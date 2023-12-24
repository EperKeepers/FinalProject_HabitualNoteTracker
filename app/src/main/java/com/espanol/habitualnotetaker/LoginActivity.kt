package com.espanol.habitualnotetaker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.espanol.habitualnotetaker.databinding.ActivityLoginBinding
import com.espanol.habitualnotetaker.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            if (binding.loginEmailEditText.text.isBlank()) {
                Toast.makeText(this, "Email must not be blank", Toast.LENGTH_SHORT).show()
            } else {
                if (binding.loginPasswordEditText.text.isBlank()) {
                    Toast.makeText(this, "Password must not be blank", Toast.LENGTH_SHORT).show()
                } else {
                    mAuth.signInWithEmailAndPassword(
                        binding.loginEmailEditText.text.toString(),
                        binding.loginPasswordEditText.text.toString()
                    ).addOnSuccessListener {
                        Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.loginRegisterTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

    }
}