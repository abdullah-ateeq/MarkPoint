package com.example.markpoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.markpoint.ui.MarkPointApp
import com.example.markpoint.ui.MarkPointViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MarkPointViewModel = viewModel(factory = MarkPointViewModel.Factory)
            MarkPointApp(viewModel)
        }
    }
}
