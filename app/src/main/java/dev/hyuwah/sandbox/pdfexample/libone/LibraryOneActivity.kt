package dev.hyuwah.sandbox.pdfexample.libone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import dev.hyuwah.sandbox.pdfexample.Constants
import dev.hyuwah.sandbox.pdfexample.R
import es.voghdev.pdfviewpager.library.RemotePDFViewPager
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter
import es.voghdev.pdfviewpager.library.remote.DownloadFile
import es.voghdev.pdfviewpager.library.util.FileUtil
import kotlinx.android.synthetic.main.activity_library_one.*
import java.io.File

class LibraryOneActivity : AppCompatActivity(), DownloadFile.Listener {

    private var url = Constants.PDF_URL
    private var adapter : PDFPagerAdapter? = null
    private lateinit var remotePDFViewPager: RemotePDFViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_one)

        btn_load_pdf.setOnClickListener {
            openRemotePdf()
            btn_load_pdf.isEnabled = false
            btn_share_pdf.isEnabled = false
            progress_bar.visibility = View.VISIBLE
            progress_bar.isIndeterminate = false
        }

        btn_load_pdf.setOnLongClickListener {
            showUrlDialog()
            true
        }

        btn_share_pdf.setOnClickListener {
            sharePdf()
        }
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
            .setNegativeButton("Cancel",null)
            .create()
        dialog.show()
    }

    private fun openRemotePdf(){
        remotePDFViewPager = RemotePDFViewPager(this, url, this)
    }

    private fun sharePdf(){
        val file = File(cacheDir, FileUtil.extractFileNameFromURL(url))
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

    private fun loadPdfToView(url: String?) {
        adapter = PDFPagerAdapter(this, FileUtil.extractFileNameFromURL(url))
        remotePDFViewPager.adapter = adapter

        linear_layout_pdf.removeAllViews()
        linear_layout_pdf.addView(
            remotePDFViewPager,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.close()
    }

    override fun onSuccess(url: String?, destinationPath: String?) {
        val file = File(cacheDir, FileUtil.extractFileNameFromURL(url)).absolutePath
        println("File exist? ${file}\n${destinationPath}")
        loadPdfToView(url)
        btn_load_pdf.isEnabled = true
        btn_share_pdf.isEnabled = true
        progress_bar.visibility = View.GONE
    }

    override fun onFailure(e: Exception?) {
        btn_load_pdf.isEnabled = true
        Toast.makeText(this, "Failed opening $url\n${e?.message?:"Unknown Exception"}", Toast.LENGTH_SHORT).show()
    }

    override fun onProgressUpdate(progress: Int, total: Int) {
        progress_bar.progress = progress
        progress_bar.max = total
    }
}
