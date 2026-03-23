package me.anyang.easyprint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.anyang.easyprint.ui.screens.HomeScreen
import me.anyang.easyprint.ui.theme.EasyPrintTheme

class MainActivity : ComponentActivity() {
    
    private var sharedFileUri by mutableStateOf<Uri?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle shared file from intent
        handleIntent(intent)
        
        setContent {
            EasyPrintTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(sharedFileUri = sharedFileUri)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("application/pdf") == true ||
                    intent.type?.startsWith("image/") == true
                ) {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    sharedFileUri = uri
                }
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    val type = contentResolver.getType(uri)
                    if (type?.startsWith("application/pdf") == true ||
                        type?.startsWith("image/") == true
                    ) {
                        sharedFileUri = uri
                    }
                }
            }
        }
    }
}
