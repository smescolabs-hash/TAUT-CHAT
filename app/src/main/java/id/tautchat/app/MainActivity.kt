package id.tautchat.app

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var profileView: View
    private lateinit var homeView: View
    private lateinit var profilePhoto: ImageView
    private var pendingPhoto: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("taut", MODE_PRIVATE)

        profileView = findViewById(R.id.profileView)
        homeView = findViewById(R.id.homeView)
        profilePhoto = findViewById(R.id.profilePhoto)

        findViewById<Button>(R.id.btnPhoto).setOnClickListener {
            val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(i, REQ_PHOTO)
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveProfile() }
        findViewById<Button>(R.id.btnAddContact).setOnClickListener {
            Toast.makeText(this, "Segera hadir: tambah kontak via QR & Bluetooth", Toast.LENGTH_SHORT).show()
        }

        if (prefs.getString("username", null) == null) showProfile() else showHome()
    }

    private fun showProfile() {
        profileView.visibility = View.VISIBLE
        homeView.visibility = View.GONE
    }

    private fun showHome() {
        profileView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
        findViewById<TextView>(R.id.homeName).text = prefs.getString("username", "")
        val f = File(filesDir, "profile.jpg")
        if (f.exists()) {
            findViewById<ImageView>(R.id.homePhoto).setImageBitmap(BitmapFactory.decodeFile(f.path))
        }
    }

    private fun saveProfile() {
        val name = findViewById<EditText>(R.id.inputName).text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Isi username dulu", Toast.LENGTH_SHORT).show()
            return
        }
        pendingPhoto?.let {
            FileOutputStream(File(filesDir, "profile.jpg")).use { out ->
                it.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
        }
        prefs.edit().putString("username", name).apply()
        showHome()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PHOTO && resultCode == RESULT_OK) {
            data?.data?.let { loadPhoto(it) }
        }
    }

    // Foto diperkecil ke 96x96 agar hemat memori dan ringan dikirim lewat Bluetooth.
    private fun loadPhoto(uri: Uri) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= SIZE && bounds.outHeight / (sample * 2) >= SIZE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return
            val side = minOf(raw.width, raw.height)
            val square = Bitmap.createBitmap(raw, (raw.width - side) / 2, (raw.height - side) / 2, side, side)
            val small = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
            pendingPhoto = small
            profilePhoto.setImageBitmap(small)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka foto", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQ_PHOTO = 1
        private const val SIZE = 96
    }
}
