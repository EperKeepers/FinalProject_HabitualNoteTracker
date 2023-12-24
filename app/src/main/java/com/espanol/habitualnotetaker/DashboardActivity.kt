package com.espanol.habitualnotetaker

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.espanol.habitualnotetaker.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var adapter: RecyclerViewAdapter

    companion object {
        val dataList = mutableListOf<NoteData>()
        var profilePicture: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = RecyclerViewAdapter(dataList, this)
        binding.recyclerView.adapter = adapter

        binding.dashboardAccountProfile.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        binding.dashboardSearchSearchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }
        })

        binding.dashboardSearchSearchView.setOnCloseListener {
            binding.dashboardSearchSearchView.setQuery("", false)
            true
        }

        mDatabase.getReference("user").child(mAuth.currentUser?.uid.toString()).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userData = snapshot.getValue(UserData::class.java) ?: UserData()
                    profilePicture = userData.profilePicture
                    binding.dashboardNameTextView.text = userData.name
                    if (userData.profilePicture == "" || userData.profilePicture == null) {
                        Glide.with(this@DashboardActivity)
                            .load(R.drawable.baseline_person_24)
                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                            .into(binding.dashboardAccountProfile)
                    } else {
                        if(!isDestroyed) {
                            Glide.with(this@DashboardActivity)
                                .load(userData.profilePicture)
                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL))
                                .into(binding.dashboardAccountProfile)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error retrieving data: $error")
            }
        })

        mDatabase.getReference("notes").addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataList.clear()
                for (snapshot in dataSnapshot.children) {
                    snapshot.getValue(NoteData::class.java)?.let { dataList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error retrieving data: $error")
            }
        })

        binding.addNoteCardView.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

    }
}