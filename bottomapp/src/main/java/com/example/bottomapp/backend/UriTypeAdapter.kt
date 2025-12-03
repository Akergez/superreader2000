package com.example.bottomapp.backend

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import android.net.Uri
import androidx.core.net.toUri

class UriTypeAdapter : TypeAdapter<Uri>() {
    override fun write(out: JsonWriter, value: Uri?) {
        out.value(value?.toString()) // Convert Uri to String
    }

    override fun read(reader: JsonReader): Uri? {
        val uriString = reader.nextString()
        return if (uriString.isEmpty()) null else uriString.toUri() // Convert String back to Uri
    }
}