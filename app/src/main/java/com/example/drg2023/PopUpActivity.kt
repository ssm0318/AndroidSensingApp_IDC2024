package com.example.drg2023

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PopUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Please open the AppMinder and answer our survey :)")
        builder.setPositiveButton("Open App") { dialog, _ ->
            dialog.dismiss()
            // Open your main app activity here
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }
        builder.setNegativeButton("Dismiss") { dialog, _ ->
            // Dismiss the dialog
            dialog.dismiss()
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }
}
