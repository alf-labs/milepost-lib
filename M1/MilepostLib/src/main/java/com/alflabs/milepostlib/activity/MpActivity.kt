package com.alflabs.milepostlib.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.alflabs.milepostlib.R
import com.alflabs.milepostlib.databinding.ActivityMpBinding

class MpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mp)
    }
}
