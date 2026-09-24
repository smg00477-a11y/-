package com.example.data.storage.archive

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object ChecksumUtility {

    fun calculateSha256(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun calculateSha256(inputStream: InputStream): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun calculateSha256(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes)
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun calculateSha256(text: String): String {
        return calculateSha256(text.toByteArray(Charsets.UTF_8))
    }

    fun verifyChecksum(file: File, expectedHash: String): Boolean {
        if (expectedHash.isBlank() || !file.exists()) return false
        val actualHash = calculateSha256(file)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
