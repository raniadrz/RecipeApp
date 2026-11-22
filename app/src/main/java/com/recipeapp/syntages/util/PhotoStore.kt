package com.recipeapp.syntages.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

object PhotoStore {
    private const val MAX_SIDE = 1600

    fun dir(context: Context): File = File(context.filesDir, "photos").apply { mkdirs() }
    fun file(context: Context, name: String): File = File(dir(context), name)

    fun authority(context: Context) = "${context.packageName}.fileprovider"

    /** Προσωρινό αρχείο για τη φωτογραφία της κάμερας. */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val d = File(context.cacheDir, "camera").apply { mkdirs() }
        val f = File(d, "cam_${System.currentTimeMillis()}.jpg")
        return f to FileProvider.getUriForFile(context, authority(context), f)
    }

    /**
     * Αντιγράφει μια εικόνα (κάμερα ή συλλογή) στον χώρο της εφαρμογής, μικραίνοντάς την και
     * διορθώνοντας τον προσανατολισμό. Επιστρέφει το όνομα του αρχείου.
     */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            val longest = max(decoded.width, decoded.height)
            if (longest > MAX_SIDE) {
                val s = MAX_SIDE.toFloat() / longest
                matrix.postScale(s, s)
            }
            val bitmap = if (matrix.isIdentity) decoded
            else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val name = "${UUID.randomUUID()}.jpg"
            file(context, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            if (bitmap !== decoded) decoded.recycle()
            bitmap.recycle()
            name
        }.getOrNull()
    }

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { file(context, name).delete() }
    }

    fun shareUri(context: Context, name: String): Uri? {
        val f = file(context, name)
        if (!f.exists()) return null
        return FileProvider.getUriForFile(context, authority(context), f)
    }
}
