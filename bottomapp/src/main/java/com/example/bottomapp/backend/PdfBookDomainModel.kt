package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream


data class PdfProgress(val pageIndex: Int) : Progress(FormatType.PDF)

class PdfBookDomainModel(
    bookLink: BookLink,
    val context: Context,
    val updateBookLink: (BookLink) -> Unit,
) :
    BookDomainModel<PdfProgress> {
    val title: String = bookLink.title
    val fileUri: Uri = bookLink.fileUri
    val filePath: String = bookLink.filePath
    var progressPath: Uri? = bookLink.progressUri

    private val gson = Gson()

    init {
        if (progressPath == null) {
            createProgressFile()
        }
    }

    private fun createProgressFile() {
        val documentId = DocumentsContract.getDocumentId(fileUri)
        // Извлекаем путь к директории (например, 'primary:Download/99536569.a4')
        val split = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val path = split[1] // Download/99536569.a4/Книга.pdf -> путь к файлу
        val directoryPath = path.substring(0, path.lastIndexOf("/")) // Получаем путь к папке
        // Теперь получаем доступ к директории
        val treeUri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",  // Провайдер
            split[0] + ":" + directoryPath
        ) // Путь до директории


        // Создаем файл opened.txt
        createFileInDirectory(treeUri, title + "_progress.json")

        val file = DocumentFile.fromTreeUri(context, fileUri)
            ?.createFile("json", title + "_progress.json")
/*        val writer = FileWriter(file!!.uri.toString())
        writer.write(gson.toJson(PdfProgress(0), PdfProgress::class.java))
        writer.close()
        updateBookLink(BookLink(title, fileUri, progressPath, filePath))*/
    }

    private fun createFileInDirectory(treeUri: Uri, fileName: String) {
        /*// Создаем DocumentFile для директории
        val directory = DocumentFile.fromTreeUri(context, treeUri)

        if (directory != null && directory.canWrite()) {
            // Проверяем, не существует ли файл already
            if (directory.findFile(fileName) == null) {
                // Создаем текстовый файл
                val createdFile = directory.createFile("text/plain", fileName)

                if (createdFile != null) {
                    // Записываем текст в файл
                    try {
                        val outputStream: OutputStream? =
                            context.getContentResolver().openOutputStream(createdFile.uri)
                        outputStream?.write("This file was opened.".toByteArray())
                        outputStream?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }*/
    }

    override fun getProgress(): PdfProgress {
        val progressFile = DocumentFile.fromTreeUri(context, progressPath!!)
        val reader = FileReader(progressFile!!.uri.toString())
        return gson.fromJson(reader, PdfProgress::class.java)
    }

    override fun getFile(): File {
        return File(fileUri.path!!)
    }

    override fun saveProgress(progress: PdfProgress) {
        /*val progressFile = DocumentFile.fromTreeUri(context, progressPath!!)
        val writer = FileWriter(progressFile!!.uri.toString())
        writer.write(gson.toJson(progress, PdfProgress::class.java))
        writer.close()*/
    }
}