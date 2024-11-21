package com.example.bottomapp.backend.exceptions

import android.content.Context
import android.widget.Toast

public class ExceptionsCatcher {
    fun dick() {}

    companion object {
        public fun <T> runAndHandleErrors(context: Context, block: () -> T): T? {
            var errorCatched: String = ""
            var res: T? = null
            try {
                res = block()
            } catch (e: InvalidStorageDirectoryException) {
                errorCatched = "хуйня а не директория, соси хуй"
            } catch (e: StorageNotSetException) {
                errorCatched = "сначала директорию выбери, потом хоть соси"
            } catch (e: FailedToCreateFileException) {
                errorCatched = "не создалось, поменяй директорию или создадим хуй в тебе"
            } catch (e: Exception) {
                errorCatched = e.message ?: e.toString()
            } finally {
                if (errorCatched != "") {
                    Toast.makeText(
                        context,
                        errorCatched,
                        Toast.LENGTH_LONG
                    ).show()
                }
                return res
            }
        }

        public fun runAndHandleErrors(context: Context, block: () -> Unit) {
            var errorCatched: String = ""
            try {
                 block()
            } catch (e: InvalidStorageDirectoryException) {
                errorCatched = "хуйня а не директория, соси хуй"
            } catch (e: StorageNotSetException) {
                errorCatched = "сначала директорию выбери, потом хоть соси"
            } catch (e: FailedToCreateFileException) {
                errorCatched = "не создалось, поменяй директорию или создадим хуй в тебе"
            } catch (e: Exception) {
                errorCatched = e.message ?: e.toString()
            } finally {
                if (errorCatched != "") {
                    Toast.makeText(
                        context,
                        errorCatched,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}