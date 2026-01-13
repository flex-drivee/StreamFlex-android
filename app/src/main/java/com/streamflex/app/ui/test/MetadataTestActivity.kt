package com.streamflex.app.ui.test

import com.streamflex.app.R
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.streamflex.app.data.repository.MetadataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MetadataTestActivity : AppCompatActivity() {

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metadata_test)

        val inputId = findViewById<EditText>(R.id.inputId)
        val btnLoadMovie = findViewById<Button>(R.id.btnLoadMovie)
        val resultView = findViewById<TextView>(R.id.resultView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnLoadMovie.setOnClickListener {
            val id = inputId.text.toString().trim()

            if (id.isEmpty()) {
                resultView.text = "Please enter an IMDB ID like: tt1234567"
                return@setOnClickListener
            }

            progressBar.visibility = android.view.View.VISIBLE
            resultView.text = ""

            uiScope.launch {
                val data = withContext(Dispatchers.IO) {
                    MetadataRepository.getMovie(id)
                }

                progressBar.visibility = android.view.View.GONE

                if (data?.meta != null) {
                    val m = data.meta
                    resultView.text = """
                        Title: ${m.name}
                        Year: ${m.year}
                        Type: ${m.type}
                        Rating: ${m.imdbRating}
                        Runtime: ${m.runtime}
                        Genres: ${m.genres?.joinToString()}
                        Description: ${m.description}
                    """.trimIndent()
                } else {
                    resultView.text = "No metadata found for that ID."
                }
            }
        }
    }
}
