package dev.hyuwah.sandbox.pdfexample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.hyuwah.sandbox.pdfexample.libone.LibraryOneActivity
import dev.hyuwah.sandbox.pdfexample.libtwo.LibraryTwoActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_lib_1.setOnClickListener { startActivity(Intent(this,LibraryOneActivity::class.java)) }
        btn_lib_2.setOnClickListener { startActivity(Intent(this,LibraryTwoActivity::class.java)) }
    }

}
