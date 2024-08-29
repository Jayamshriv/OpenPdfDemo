package com.example.openpdfdemo

import android.content.Context
import android.os.Environment
import com.lowagie.text.Chunk
import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfString
import com.lowagie.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HelloWorld {
    companion object {

        fun hwlloePDF() = runBlocking {
            println("Hello World")
            val document = Document()

            withContext(Dispatchers.IO) {
                try {
                    // Step 1: Check if we can write to the Downloads directory
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.canWrite()) {
                        throw IOException("Cannot write to Downloads directory")
                    }

                    // Step 2: Create the file path in the Downloads directory
                    val pdfFile = File(downloadsDir, "hello_world.pdf")

                    // Step 3: Creating a writer that listens to the document and directs a PDF stream to the file
                    val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))

                    // Step 4: Opening the document
                    document.open()
                    writer.info.put(PdfName.CREATOR, PdfString(Document.getVersion()))

                    // Step 5: Adding a paragraph to the document
                    document.add(Paragraph("Hello World"))

                    writer.flush()
                    println("PDF saved to: ${pdfFile.absolutePath}")

                } catch (de: DocumentException) {
                    System.err.println(de.message)
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }

            }
        }

        fun helloPDF() {
            val pdfOutputFile =
                FileOutputStream(Environment.getExternalStoragePublicDirectory("hello"))

            //1) Configure the layout of the document by adding
            //   parameters to the constructor (size and margins)
            val myPDFDoc = Document(
                PageSize.A4,
                40f,   // left
                40f,   // right
                200f,  // top
                150f
            ) // down

            //2) Create immutable variables for the footer that will display
            //.  a rectangle in color black
            val footer = Rectangle(30f, 30f, PageSize.A4.getRight(30f), 140f).apply {
                border = Rectangle.BOX

                borderWidth = 2f
            }

            // 3) Create an immutable variable header (rectangle in color blue)
            val header = Rectangle(30f, 30f, PageSize.A4.getRight(30f), 140f).apply {
                border = Rectangle.BOX
                borderWidth = 1f
                top = PageSize.A4.getTop(30f)
                bottom = PageSize.A4.getTop(180f)
            }

            val title = "Learning OpenPDF with Kotlin"

            val lorenIpsum1 =
                """Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo 
    |ligula eget dolor. Aenean massa. 
    |parturient montes, nascetur ridiculus mus. 
        
    |justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. 
    |Cras dapibus. Vivamus elementum semper nisi. 
    |Aenean vulputate eleifend tellus."""
                    .trimMargin()

            val pdfWriter = PdfWriter.getInstance(myPDFDoc, pdfOutputFile).apply {

                // 4) Use an object expression to configure and register page event
                // in charge of adding the header and the footer
                setPageEvent(object : PdfPageEventHelper() {

                    // Override the method onEndPage
                    override fun onEndPage(writer: PdfWriter, doc: Document) {
                        with(writer.getDirectContent()) {
                            rectangle(header);
                            rectangle(footer);
                        }
                    }
                })
            }

            myPDFDoc.apply {

                addTitle("This is a simple PDF example")
                addSubject("This is a tutorial explaining how to use openPDF")
                addKeywords("Kotlin, OpenPDF, Basic sample")
                addCreator("Miguel and Kesizo.com")
                addAuthor("Miguel Doctor")

                open()
                add(
                    Paragraph(title, Font(Font.COURIER, 20f, Font.BOLDITALIC)).apply {
                        alignment = Element.ALIGN_CENTER
                    }
                )

                add(Paragraph(Chunk.NEWLINE))
                add(Paragraph(lorenIpsum1))
                close()
            }

            pdfWriter.close() // close the File writer
        }

        fun alalala(context: Context) {
            println("Hello World")

            val document = Document()
            try {
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val pdfFile = File(downloadsDir, "hello_world.pdf")

                val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFile))

                document.open()
                writer.info.put(PdfName.CREATOR, PdfString(Document.getVersion()))

                document.add(Paragraph("Hello World"))

                println("PDF saved to: ${pdfFile.absolutePath}")
            } catch (de: DocumentException) {
                System.err.println(de.message)
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            } finally {
                // Step 5: Closing the document
                document.close()
            }

        }

    }
}
