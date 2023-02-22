package com.covilledesigns.signatureapptwo

import android.Manifest
import android.app.Activity
import com.github.gcacace.signaturepad.views.SignaturePad
import android.os.Bundle
import android.widget.Toast
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.os.Environment
import kotlin.Throws
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat
import java.io.*
import java.util.*

class MainActivity : Activity() {
    private var mSignaturePad: SignaturePad? = null
    private var mClearButton: Button? = null
    private var mSaveButton: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyStoragePermissions(this)
        setContentView(R.layout.activity_main)
        mSignaturePad = findViewById<View>(R.id.signature_pad) as SignaturePad
        mSignaturePad!!.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {
                Toast.makeText(this@MainActivity, "OnStartSigning", Toast.LENGTH_SHORT).show()
            }
            override fun onSigned() {
                mSaveButton!!.isEnabled = true
                mClearButton!!.isEnabled = true
            }
            override fun onClear() {
                mSaveButton!!.isEnabled = false
                mClearButton!!.isEnabled = false
            }
        })
        mClearButton = findViewById<View>(R.id.clear_button) as Button
        mSaveButton = findViewById<View>(R.id.save_button) as Button
        mClearButton!!.setOnClickListener { mSignaturePad!!.clear() }
        mSaveButton!!.setOnClickListener {
            val signatureBitmap = mSignaturePad!!.signatureBitmap

            if (addJpgSignatureToGallery(signatureBitmap)) {
                Toast.makeText(this, "Signature saved into the Gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Unable to store the signature", Toast.LENGTH_SHORT).show()
            }

            /*
            if (addSvgSignatureToGallery(mSignaturePad!!.signatureSvg)) {
                Toast.makeText(this, "SVG Signature saved into the Gallery", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Unable to store the SVG signature", Toast.LENGTH_SHORT).show()
            }
            */
        }

        //END onCreate() function
    }

    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<String>, grantResults: IntArray)
    {
        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(this@MainActivity, "Cannot write images to external storage", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun getAlbumStorageDir(albumName: String): File {
        // Get the directory for the user's public pictures directory.
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), albumName
        )
        if (!file.mkdirs()) {
            Log.e("SignaturePad", "Directory not created")
        }
        return file
    }

    @Throws(IOException::class)
    fun saveBitmapToJPG(bitmap: Bitmap, photo: File?) {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val stream: OutputStream = FileOutputStream(photo)
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.close()
    }

    private fun addJpgSignatureToGallery(signature: Bitmap): Boolean {
        var result = false
        try {
            val photo = File(
                getAlbumStorageDir("SignaturePad"),
                String.format("Signature_%d.jpg", System.currentTimeMillis())
            )
            saveBitmapToJPG(signature, photo)
            scanMediaFile(photo)
            result = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }

    private fun scanMediaFile(photo: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(photo)
        mediaScanIntent.data = contentUri
        this@MainActivity.sendBroadcast(mediaScanIntent)
    }

    private fun addSvgSignatureToGallery(signatureSvg: String?): Boolean {
        var result = false
        try {
            val svgFile = File(
                getAlbumStorageDir("SignaturePad"),
                String.format("Signature_%d.svg", System.currentTimeMillis())
            )
            val stream: OutputStream = FileOutputStream(svgFile)
            val writer = OutputStreamWriter(stream)
            writer.write(signatureSvg)
            writer.close()
            stream.flush()
            stream.close()
            scanMediaFile(svgFile)
            result = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        fun verifyStoragePermissions(activity: Activity) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }

    //END MainActivity CLASS
}