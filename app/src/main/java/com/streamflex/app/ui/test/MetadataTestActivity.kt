package com.streamflex.app.ui.test

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.streamflex.app.R
import com.streamflex.app.data.repository.MetadataRepository
import kotlin.concurrent.thread

class MetadataTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metadata_test)

        val inputId = findViewById<EditText>(R.id.inputId)
        val btnFetch = findViewById<Button>(R.id.btnFetch)
        val resultView = findViewById<TextView>(R.id.resultView)

        btnFetch.setOnClickListener {
            val imdb = inputId.text.toString().trim()
            if (imdb.isEmpty()) return@setOnClickListener

            thread {
                val result = MetadataRepository.getMovie(imdb)
                runOnUiThread {
                    resultView.text = if (result?.meta != null) {
                        "Title: ${result.meta.name}\n" +
                                "Year: ${result.meta.year}\n" +
                                "Rating: ${result.meta.imdbRating}"
                    } else {
                        "No data found"
                    }
                }
            }
        }
    }
}
