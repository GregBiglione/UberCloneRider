package com.greg.uberclonerider

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream

class SavePhoto {

    //----------------------------------------------------------------------------------------------
    //------------------- Get Image from Uri in Media Store ----------------------------------------
    //----------------------------------------------------------------------------------------------

    fun getImageUri(context: Context, bitmapInStorage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        val id: Long = System.currentTimeMillis()
        bitmapInStorage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmapInStorage, "Photo$id", null)
        return Uri.parse(path)
    }
}