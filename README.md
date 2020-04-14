# Android PDF Example

Sandbox project on how to display PDF File (from url) and share pdf file with Android Sharesheet

## Method 1 (PdfViewPager)

This library will auto download remote pdf, and display on custom viewpager

### Library Used

I'm using [PdfViewPager](https://github.com/voghDev/PdfViewPager) by [voghDev](https://github.com/voghDev)

### Code Walkthrough

#### Setup Library

Add `implementation 'es.voghdev.pdfviewpager:library:1.1.0'` on `app/build.gradle`

Add permission needed by the library
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

#### Setup View

If pdf source is from url, we need to use `RemotePDFViewPager` and we're gonna need to add it to our layout programmatically

In this example we use LinearLayout as the container

```xml
 <LinearLayout
        android:id="@+id/linear_layout_pdf"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottom_bar"/>
```

#### Setup Code

On activity (or fragment), we need 4 main component

1. A valid PDF url
2. A PDFPagerAdapter
3. A RemotePDFViewPager
4. The listener for RemotePDFViewPager's DownloadFile

```kotlin
// MainActivity.kt that implement DownloadFile.Listener

private var url = "https://www.btpn.com/pdf/investor/annual-report/in/btpnar2017_ind_r.pdf"
private var adapter : PDFPagerAdapter? = null
private lateinit var remotePDFViewPager: RemotePDFViewPager

override fun onCreate(savedInstanceState: Bundle?) [
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    remotePDFViewPager = RemotePDFViewPager(this, url, this) // <- This will start the download
}

override fun onDestroy() {
    super.onDestroy()
    adapter?.close() // Don't forget to cleanup the adapter
}

override fun onSuccess(url: String?, destinationPath: String?) {
    adapter = PDFPAgerAdapter(this, FileUtil.extractFileNameFromURL(url)) // Setup adapter with the file
    remotePDFViewPager.adapter = adapter // Attach adapter to remote pdf viewpager

    // Add it to the container
    linear_layout_pdf.removeAllViews()
    linear_layou_pdf.addView(
        remotePDFViewPager,
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    )
}

override fun onFailure(e: Exception?) {
    // Handle pdf download failure
}

override fun onProgressUpdate(progress: Int, total: Int) {
    // Update pdf download progress
}
```

#### Sharing PDF

To be able to share pdf (or file), we need to use / setup FileProvider on the app

Create `provider_paths.xml` on `res/xml`
```xml
<!-- Provider Path (provider_paths.xml) --> 
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path
        name="external-files"
        path="."/>
        <!-- Need to add line below, because lib downloads the pdf into cache -->
    <cache-path
        name="cache-files"
        path="."/>
</paths>
```

Add provider tag on AndroidManifest
```xml
<!-- AndroidManifest.xml -->
<manifest>
    <application>

        ...

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/provider_paths"/>
        </provider>

        ...

    </application>
</manifest>
```

And here's the code to share the file using Android Sharesheet. By default, the PdfViewPager library downloads the file into app's cache directory.

```kotlin
val file = File(cacheDir, FileUtil.extractFileNameFromURL(url))
if (file.exists()) {
    // Generate file Uri
    val fileUri = FileProvider.getUriForFile(
            this,
            this.applicationContext.packageName + ".provider",
            file
    )
    // Create Share Intent with the file uri
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, fileUri)
        type = "application/pdf"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, "Kirim PDF ke..."))
}
```

### Preview

![](https://res.cloudinary.com/hyuwah-github-io/image/upload/v1583224491/sandbox-pdf-preview_qr4usx.gif)

## Method 2 (PdfView)

This library doesn't handle downloading remote pdf, it only shows pdf file
We have to implement our own file download logic

### Library Used

I'm using [PdfView-android](https://github.com/Dmitry-Borodin/pdfview-android) by [Dmitry-Borodin](https://github.com/Dmitry-Borodin)

### Code Walkthrough

TODO