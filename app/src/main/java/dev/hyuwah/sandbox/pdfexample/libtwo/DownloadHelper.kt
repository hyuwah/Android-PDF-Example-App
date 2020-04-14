package dev.hyuwah.sandbox.pdfexample.libtwo

import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.Context.DOWNLOAD_SERVICE
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.hyuwah.sandbox.pdfexample.BuildConfig
import java.io.File

/**
 * Created by [Bobby.Hardian](https://github.com/hardbobby) on 02/03/2020.
 */
object DownloadHelper {

    const val EXTENSIONS_PDF = ".pdf"
    const val FOLDER_E_TICKET = "/E-TICKET/"

    /**
     * Used to download the file from url.
     *
     *
     * 1. Download the file using Download Manager.
     *
     * @param url      Url.
     * @param listener Listener.
     */
    fun downloadFile(activity: Activity, url: String?, isShare: Boolean, listener: DownloadListener) {
        try {
            if (url?.isNotEmpty() == true) {
                val uri: Uri = Uri.parse(url)
                activity.registerReceiver(
                    receiverResponseDownload(listener, isShare), IntentFilter(
                        DownloadManager.ACTION_DOWNLOAD_COMPLETE
                    )
                )
                val fileName = generateFileName(url.orEmpty()) + EXTENSIONS_PDF
                val file =
                    File(Environment.getExternalStorageDirectory().path + "/" + Environment.DIRECTORY_DOWNLOADS + "$FOLDER_E_TICKET$fileName")
                if (!isFilePdfExist(file.path)) {
                    val folder = Environment.DIRECTORY_DOWNLOADS + FOLDER_E_TICKET
                    val subFolder = File(folder)
                    if (!subFolder.exists()) {
                        subFolder.mkdirs()
                    }
                    val request = DownloadManager.Request(uri)
                    request.setTitle(fileName);
                    request.setDescription("Downloading attachment..");
                    request.setMimeType(getMimeTypeFromUrl(uri.toString()))
                    request.allowScanningByMediaScanner()
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "$FOLDER_E_TICKET$fileName"
                    );
                    val dm = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                } else if (isShare) {
                    shareDownloadAttachment(activity, Uri.fromFile(file), getMimeTypeFromExtension(file.absolutePath))
                } else {
                    openDownloadedAttachment(
                        activity,
                        Uri.fromFile(file),
                        getMimeTypeFromExtension(file.absolutePath)
                    )
                }
            }
        } catch (e: IllegalStateException) {
            Toast.makeText(
                activity,
                "Please insert an SD card to download file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     *
     */
    fun getDownloadedPdfFile(url: String): File {
        val fileName = generateFileName(url) + EXTENSIONS_PDF
        return File(Environment.getExternalStorageDirectory().path + "/" + Environment.DIRECTORY_DOWNLOADS + "$FOLDER_E_TICKET$fileName")
    }

    /**
     * Used to get MimeType from url.
     *
     * @param url Url.
     * @return Mime Type for the given url.
     */
    private fun getMimeTypeFromUrl(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            val mime = MimeTypeMap.getSingleton()
            type = mime.getMimeTypeFromExtension(extension)
        }
        return type
    }

    private fun getMimeTypeFromExtension(path: String): String {
        val file = File(path)
        val extensionFile =
            file.absolutePath.substring(file.absolutePath.lastIndexOf(".")).replace(".", "")
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getMimeTypeFromExtension(extensionFile).orEmpty()
    }

    /**
     * Used to get MimeType from url.
     *
     * @param url Url.
     * @return fileName
     */
    private fun generateFileName(url: String): String {
        return url.substring(url.lastIndexOf('/') + 1).replace(".", "")
    }

    /**
     * Attachment download complete receiver.
     *
     *
     * 1. Receiver gets called once attachment download completed.
     * 2. Open the downloaded file.
     */
    private fun receiverResponseDownload(listener: DownloadListener, isShare: Boolean): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                    val downloadId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, 0
                    )
                    receiveStatusDownload(context, downloadId, listener, isShare)
                }
            }
        }
    }

    /**
     * Used to open the downloaded attachment.
     *
     * @param context    Content.
     * @param downloadId Id of the downloaded file to open.
     */
    private fun receiveStatusDownload(
        context: Context,
        downloadId: Long,
        listener: DownloadListener,
        isShare: Boolean
    ) {
        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        val cursor: Cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val downloadLocalUri =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val downloadMimeType =
                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
            when (downloadStatus) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType)
                    listener.statusSuccess()
                    if (isShare) {
                        shareDownloadAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType)
                    } else {
                        openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType)
                    }
                }
                DownloadManager.STATUS_FAILED -> {
                    listener.statusFailed()
                }
                DownloadManager.STATUS_PAUSED -> {
                    listener.statusFailed()
                }
                DownloadManager.STATUS_PENDING -> {
                    listener.statusFailed()
                }
            }

        }
        cursor.close()
    }

    /**
     * Used to open the downloaded attachment.
     *
     *
     * 1. Fire intent to open download file using external application.
     *
     * 2. Note:
     * 2.a. We can't share fileUri directly to other application (because we will get FileUriExposedException from Android7.0).
     * 2.b. Hence we can only share content uri with other application.
     * 2.c. We must have declared FileProvider in manifest.
     * 2.c. Refer - https://developer.android.com/reference/android/support/v4/content/FileProvider.html
     *
     * @param context            Context.
     * @param attachmentUri      Uri of the downloaded attachment to be opened.
     * @param attachmentMimeType MimeType of the downloaded attachment.
     */
    private fun openDownloadedAttachment(
        context: Context,
        attachmentUri: Uri?,
        attachmentMimeType: String
    ) {
        var attachmentUri: Uri? = attachmentUri
        if (attachmentUri != null) { // Get Content Uri.
            if (ContentResolver.SCHEME_FILE == attachmentUri.scheme) { // FileUri - Convert it to contentUri.
                val file = File(attachmentUri.path)
                attachmentUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )
            }
            val openAttachmentIntent = Intent(Intent.ACTION_VIEW)
            openAttachmentIntent.setDataAndType(attachmentUri, attachmentMimeType)
            openAttachmentIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.startActivity(openAttachmentIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "R.string.system_error", Toast.LENGTH_LONG).show()
            }
        }
    }


    /**
     * Used to open the downloaded attachment.
     *
     *
     * 1. Fire intent to open download file using external application.
     *
     * 2. Note:
     * 2.a. We can't share fileUri directly to other application (because we will get FileUriExposedException from Android7.0).
     * 2.b. Hence we can only share content uri with other application.
     * 2.c. We must have declared FileProvider in manifest.
     * 2.c. Refer - https://developer.android.com/reference/android/support/v4/content/FileProvider.html
     *
     * @param context            Context.
     * @param attachmentUri      Uri of the downloaded attachment to be opened.
     * @param attachmentMimeType MimeType of the downloaded attachment.
     */
    private fun shareDownloadAttachment(context: Context, attachmentUri: Uri?, attachmentMimeType: String) {
        var attachmentUri: Uri? = attachmentUri
        if (attachmentUri != null) { // Get Content Uri.
            if (ContentResolver.SCHEME_FILE == attachmentUri.scheme) { // FileUri - Convert it to contentUri.
                val file = File(attachmentUri.path)
                attachmentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)
            }
            val openAttachmentIntent = Intent(Intent.ACTION_SEND).apply {
                type = attachmentMimeType
                putExtra(Intent.EXTRA_STREAM, attachmentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Share File Anda Ke .....")
                putExtra(Intent.EXTRA_TEXT, "Share File Anda Ke .....")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            try {
//                context.startActivity(openAttachmentIntent)
                context.startActivity(Intent.createChooser(openAttachmentIntent, "R.string.msg_share_to"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "R.string.system_error", Toast.LENGTH_LONG).show()
            }
        }
    }
    /* private fun saveThumbnailImage(): Uri {
         val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
         val cachePath = File(applicationContext.cacheDir, imageCacheDir)
         cachePath.mkdirs()
         val stream = FileOutputStream("$cachePath/$imageFile")
         bm.compress(Bitmap.CompressFormat.PNG, 100, stream)
         stream.close()
         val imagePath = File(cacheDir, imageCacheDir)
         val newFile = File(imagePath, imageFile)
         return FileProvider.getUriForFile(this, fileProviderAuthority, newFile)
     }*/

    private fun isFilePdfExist(filePath: String): Boolean {
        return File(filePath).exists()
    }


    interface DownloadListener {
        fun statusSuccess()

        fun statusFailed()
    }
}