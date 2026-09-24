package com.example.data.storage.archive

import android.content.Context
import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Random

data class SenderSession(
    val serverIp: String,
    val serverPort: Int,
    val pairingPin: String,
    val documentCount: Int,
    val archiveFile: File
)

class LocalDeviceTransferManager(
    private val context: Context,
    private val database: AppDatabase,
    private val archiveManager: RaqeemArchiveManager
) {

    private var serverSocket: ServerSocket? = null
    private var isServerRunning = false

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    suspend fun startSenderSession(
        selectedDocumentIds: List<Long>? = null,
        port: Int = 8888,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): SenderSession? = withContext(Dispatchers.IO) {
        stopSenderSession()

        val archiveFile = File(context.cacheDir, "transfer_export_${System.currentTimeMillis()}.rqm")
        val exportedFile = archiveManager.exportLibraryArchive(selectedDocumentIds, archiveFile, onProgress)
            ?: return@withContext null

        val localIp = getLocalIpAddress()
        val pin = String.format("%04d", Random().nextInt(10000))

        try {
            serverSocket = ServerSocket(port)
            isServerRunning = true

            val targetDocsCount = selectedDocumentIds?.size ?: database.documentDao().getAllDocumentsDirect().size

            val session = SenderSession(
                serverIp = localIp,
                serverPort = port,
                pairingPin = pin,
                documentCount = targetDocsCount,
                archiveFile = exportedFile
            )

            // Start listening in background loop
            startServerThread(session)

            session
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun startServerThread(session: SenderSession) {
        Thread {
            while (isServerRunning && serverSocket?.isClosed == false) {
                try {
                    val socket: Socket = serverSocket?.accept() ?: break
                    handleClientRequest(socket, session)
                } catch (e: Exception) {
                    if (!isServerRunning) break
                }
            }
        }.start()
    }

    private fun handleClientRequest(socket: Socket, session: SenderSession) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = DataOutputStream(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val pinParam = requestLine.substringAfter("pin=", "").substringBefore(" ")

            if (pinParam == session.pairingPin) {
                out.writeBytes("HTTP/1.1 200 OK\r\n")
                out.writeBytes("Content-Type: application/octet-stream\r\n")
                out.writeBytes("Content-Length: ${session.archiveFile.length()}\r\n\r\n")

                FileInputStream(session.archiveFile).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                    }
                }
                out.flush()
            } else {
                out.writeBytes("HTTP/1.1 401 Unauthorized\r\n\r\nPIN Incorrect")
                out.flush()
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSenderSession() {
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null
    }

    suspend fun receiveFromDevice(
        senderIp: String,
        port: Int = 8888,
        pairingPin: String,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.KEEP_BOTH_CREATE_COPY,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): ImportResult = withContext(Dispatchers.IO) {
        val targetFile = File(context.cacheDir, "transfer_import_${System.currentTimeMillis()}.rqm")

        try {
            onProgress(0.1f, "الاتصال بالجهاز المرسل على $senderIp...")
            val socket = Socket(InetAddress.getByName(senderIp), port)

            val out = PrintWriter(socket.getOutputStream(), true)
            val input = socket.getInputStream()

            out.println("GET /download?pin=$pairingPin HTTP/1.1")
            out.println("Host: $senderIp")
            out.println("Connection: close")
            out.println()

            // Skip HTTP headers
            val reader = BufferedReader(InputStreamReader(input))
            var line: String? = reader.readLine()
            var isHeader = true
            while (line != null && isHeader) {
                if (line.isEmpty()) {
                    isHeader = false
                } else if (line.contains("401 Unauthorized")) {
                    socket.close()
                    return@withContext ImportResult(0, 0, 0, false, "رمز الاقتران (PIN) غير صحيح")
                }
                line = reader.readLine()
            }

            onProgress(0.4f, "جاري استقبال البيانات عبر الشبكة المحلية...")
            FileOutputStream(targetFile).use { fos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                }
            }
            socket.close()

            onProgress(0.7f, "استكمال استيراد الأرشيف إلى المكتبة المحلية...")
            val result = archiveManager.importArchive(targetFile, duplicateStrategy, onProgress)
            targetFile.delete()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            targetFile.delete()
            ImportResult(0, 0, 0, false, "فشل الاتصال أو نقل البيانات: ${e.localizedMessage}")
        }
    }
}
