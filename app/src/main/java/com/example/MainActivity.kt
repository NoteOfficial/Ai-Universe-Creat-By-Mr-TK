package com.example

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.ui.navigation.AiUniverseRoot
import com.example.ui.viewmodel.MainViewModel
import java.io.File

class MainActivity : ComponentActivity() {
  private val mainViewModel: MainViewModel by viewModels { MainViewModel.Factory(application) }

  private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

  private val webViewFilePicker =
    registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
      val cb = pendingFileCallback
      pendingFileCallback = null
      if (uris.isNotEmpty()) {
        cb?.onReceiveValue(uris.toTypedArray())
      } else {
        cb?.onReceiveValue(null)
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as AiUniverseApp
    app.webViewManager.onFileChooserRequested = { callback ->
      pendingFileCallback?.onReceiveValue(null)
      pendingFileCallback = callback
      webViewFilePicker.launch(arrayOf("*/*"))
    }

    app.webViewManager.onDownloadRequested = { url, mimeType, filename ->
      when {
        url.startsWith("http://") || url.startsWith("https://") -> {
          runCatching {
            val request =
              DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                setTitle(filename)
                setDescription("Downloading via AI UNIVERSE")
                setNotificationVisibility(
                  DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
              }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            mainViewModel.recordDownloadActivity(filename, mimeType, url)
            Toast.makeText(this, "Downloading $filename...", Toast.LENGTH_SHORT).show()
          }.onFailure {
            Toast.makeText(this, "Download failed: ${it.localizedMessage}", Toast.LENGTH_SHORT)
              .show()
          }
        }
        url.startsWith("data:") -> {
          runCatching {
            val commaIdx = url.indexOf(',')
            if (commaIdx > 0) {
              val b64 = url.substring(commaIdx + 1)
              val bytes = Base64.decode(b64, Base64.DEFAULT)
              val outDir = File(filesDir, "vault").apply { mkdirs() }
              val safeName = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
              File(outDir, safeName).writeBytes(bytes)
              mainViewModel.recordDownloadActivity(safeName, mimeType, "Saved to File Vault")
              Toast.makeText(this, "Saved $safeName to File Manager", Toast.LENGTH_SHORT).show()
            }
          }.onFailure {
            Toast.makeText(this, "Could not save data download", Toast.LENGTH_SHORT).show()
          }
        }
        else -> {
          Toast.makeText(
            this,
            "Blob downloads require opening this AI in Chrome Custom Tab.",
            Toast.LENGTH_LONG,
          ).show()
        }
      }
    }

    setContent { AiUniverseRoot(viewModel = mainViewModel) }
  }
}
