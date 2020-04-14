package dev.hyuwah.sandbox.pdfexample.libtwo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import dev.hyuwah.sandbox.pdfexample.Constants
import dev.hyuwah.sandbox.pdfexample.R
import kotlinx.android.synthetic.main.activity_library_two.*


class LibraryTwoActivity : AppCompatActivity() {

    private var url = Constants.PDF_URL

    // Storage Permissions
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    fun verifyStoragePermissions(activity: Activity) { // Check if we have write permission
        val permission =
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        if (permission != PackageManager.PERMISSION_GRANTED) { // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_two)

        // Check Storage permission
        verifyStoragePermissions(this)

        // Download pdf
        val listener = object : DownloadHelper.DownloadListener {
            override fun statusSuccess() {
                btn_share_pdf.isEnabled = true
                btn_load_pdf.isEnabled = true

                val file = DownloadHelper.getDownloadedPdfFile(url)
                if(file.exists()){
                    pdf_view.fromFile(file).show()
                }
            }
            override fun statusFailed() {
                btn_load_pdf.isEnabled = true
                btn_share_pdf.isEnabled = false
                println("Download Failed")
            }
        }

        btn_load_pdf.setOnClickListener {
            btn_load_pdf.isEnabled = false
            val file = DownloadHelper.getDownloadedPdfFile(url)
            if(file.exists()){
                btn_share_pdf.isEnabled = true
                pdf_view.fromFile(file).show()
            }else{
                DownloadHelper.downloadFile(this, url, false, listener)
            }
        }

        btn_load_pdf.setOnLongClickListener {
            showUrlDialog()
            true
        }

        btn_share_pdf.setOnClickListener {
            sharePdf()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showUrlDialog() {
        val urlEditText = EditText(this).apply {
            setText(url)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Change PDF Url")
            .setView(urlEditText)
            .setPositiveButton("Change") { _, _ ->
                url = urlEditText.text.toString()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun sharePdf() {
        val file = DownloadHelper.getDownloadedPdfFile(url)
        if (file.exists()) {
            val fileUri = FileProvider.getUriForFile(
                this,
                this.applicationContext.packageName + ".provider",
                file
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Kirim PDF ke..."))
        }
    }

}