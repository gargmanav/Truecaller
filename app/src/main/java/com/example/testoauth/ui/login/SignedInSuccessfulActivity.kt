package com.example.testoauth.ui.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.testoauth.databinding.ActivitySignedInSuccessfulBinding

class SignedInSuccessfulActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignedInSuccessfulBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignedInSuccessfulBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val name = intent.getStringExtra("name")
        name?.let { binding.welcomeText.text = String.format("Welcome, %s!", it) }
    }
}
