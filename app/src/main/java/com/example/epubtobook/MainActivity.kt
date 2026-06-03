package com.example.epubtobook

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var hiddenWebView: WebView

    // This opens the file picker when the button is clicked
    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { convertEpubToPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hiddenWebView = WebView(this) // The "engine" that builds the PDF

        findViewById<Button>(R.id.btnSelect).setOnClickListener {
            pickerLauncher.launch("application/epub+zip")
        }
    }

    private fun convertEpubToPdf(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val book = EpubReader().readEpub(inputStream)
            val htmlBuilder = StringBuilder()

            // START OF THE "PUBLISHING COMPANY" STYLING
            htmlBuilder.append("<html><head><style>")
            htmlBuilder.append("""
                @page { 
                    size: 5.5in 8.5in; 
                    margin: 0.8in 0.6in; 
                }
                body { 
                    font-family: 'serif'; 
                    text-align: justify; 
                    line-height: 1.6; 
                    font-size: 11pt; 
                }
                h1, h2 { 
                    text-align: center; 
                    text-transform: uppercase; 
                    margin-top: 100px; 
                    page-break-before: always; 
                }
                p { margin: 0; text-indent: 1.5em; }
                /* The "Drop Cap" for a professional look */
                h1 + p:first-letter, h2 + p:first-letter {
                    float: left; font-size: 3.5em; line-height: 0.8;
                    font-weight: bold; margin-right: 8px;
                }
            """.trimIndent())
            htmlBuilder.append("</style></head><body>")

            // Read every chapter from the EPUB
            for (resource in book.contents) {
                val chapterHtml = Jsoup.parse(String(resource.data)).body().html()
                htmlBuilder.append("<div class='chapter'>$chapterHtml</div>")
            }

            htmlBuilder.append("</body></html>")

            // Create the PDF
            sendToPrinter(htmlBuilder.toString())

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendToPrinter(fullHtml: String) {
        hiddenWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view!!.createPrintDocumentAdapter("MyConvertedBook")
                
                val attrs = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.NA_INDEX_5X8)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager.print("Book Job", adapter, attrs)
            }
        }
        hiddenWebView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
    }
}
