package com.heshan.tvsraider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.util.ArrayList

/**
 * Lets other apps read an exported log zip file securely.
 */
class LogFileProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.heshan.tvsraider.logs"

        fun dir(c: Context): File = File(c.cacheDir, "logs")

        fun uriFor(f: File): Uri = Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(f.name).build()
    }

    private fun fileFor(uri: Uri): File? {
        val name = uri.lastPathSegment ?: return null
        if (name.contains("/") || name.contains("\\") || name.startsWith(".")) return null
        val f = File(dir(context ?: return null), name)
        return if (f.isFile) f else null
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = fileFor(uri) ?: throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        sel: String?,
        args: Array<String>?,
        sort: String?
    ): Cursor? {
        val f = fileFor(uri) ?: return null
        val proj = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cols = ArrayList<String>()
        val vals = ArrayList<Any>()
        for (col in proj) {
            if (OpenableColumns.DISPLAY_NAME == col) {
                cols.add(col)
                vals.add(f.name)
            } else if (OpenableColumns.SIZE == col) {
                cols.add(col)
                vals.add(f.length())
            }
        }
        val c = MatrixCursor(cols.toTypedArray(), 1)
        c.addRow(vals.toTypedArray())
        return c
    }

    override fun getType(uri: Uri): String {
        val name = uri.lastPathSegment
        return if (name != null && name.endsWith(".zip")) "application/zip" else "text/plain"
    }

    override fun insert(uri: Uri, v: ContentValues?): Uri? = throw UnsupportedOperationException("read-only")
    override fun delete(uri: Uri, s: String?, a: Array<String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?): Int = 0
}
