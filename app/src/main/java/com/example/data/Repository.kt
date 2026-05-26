package com.example.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.FilterInputStream
import java.io.InputStream
import java.net.InetAddress

class ProgressInputStream(
    private val inStream: InputStream,
    private val totalBytes: Long,
    private val onProgress: (Float) -> Unit
) : FilterInputStream(inStream) {
    private var bytesRead: Long = 0

    override fun read(): Int {
        val b = inStream.read()
        if (b != -1) {
            bytesRead++
            notifyProgress()
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = inStream.read(b, off, len)
        if (read != -1) {
            bytesRead += read
            notifyProgress()
        }
        return read
    }

    private fun notifyProgress() {
        if (totalBytes > 0) {
            val progress = bytesRead.toFloat() / totalBytes.toFloat()
            onProgress(progress.coerceIn(0f, 1f))
        }
    }
}

class AppRepository(
    private val context: Context,
    private val profileDao: ProfileDao,
    private val transferLogDao: TransferLogDao
) {
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()
    val recentLogs: Flow<List<TransferLogEntity>> = transferLogDao.getRecentLogs()

    suspend fun saveProfile(profile: ProfileEntity): Long = withContext(Dispatchers.IO) {
        profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        profileDao.deleteProfile(profile)
    }

    suspend fun insertTransferLog(log: TransferLogEntity) = withContext(Dispatchers.IO) {
        transferLogDao.insertLog(log)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        transferLogDao.clearLogs()
    }

    fun getUriMetadata(uri: Uri): Pair<Long, String> {
        var size: Long = 0
        var name = "icon0.png"
        try {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex) ?: "icon0.png"
                    }
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Failed to retrieve Uri metadata", e)
        }
        return Pair(size, name)
    }

    suspend fun performFtpUpload(
        uri: Uri,
        ip: String,
        port: Int,
        remotePath: String,
        titleId: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val (totalBytes, fileName) = getUriMetadata(uri)
        val ftpClient = FTPClient()
        var inputStream: InputStream? = null
        var progressStream: ProgressInputStream? = null
        
        try {
            // Set network timeouts (connection: 7s, data socket timeout: 15s)
            ftpClient.connectTimeout = 7000
            ftpClient.defaultTimeout = 7000
            
            Log.d("Repository", "Connecting to FTP: $ip:$port")
            ftpClient.connect(ip, port)
            
            // Limit socket read delay once connected
            ftpClient.soTimeout = 15000
            
            val loginSuccess = ftpClient.login("anonymous", "")
            if (!loginSuccess) {
                return@withContext Result.failure(Exception("FTP server rejected login (anonymous access failed)"))
            }
            
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext Result.failure(Exception("Could not open input stream from selected file"))
            }
            
            progressStream = ProgressInputStream(inputStream, totalBytes, onProgress)
            
            // To be extremely safe with nested directories, PS4 FTP payload might require 
            // the full remote path to exist, or we simply let storeFile attempt write.
            // Under normal circumstances, /user/appmeta/{TITLE_ID}/ exists after the game is installed on PS4.
            Log.d("Repository", "Saving icon to path: $remotePath")
            
            val uploaded = ftpClient.storeFile(remotePath, progressStream)
            
            if (uploaded) {
                // Log success to database
                insertTransferLog(
                    TransferLogEntity(
                        ipAddress = ip,
                        titleId = titleId,
                        fileName = fileName,
                        destinationPath = remotePath,
                        sizeBytes = totalBytes,
                        status = "SUCCESS"
                    )
                )
                Result.success(Unit)
            } else {
                val replyString = ftpClient.replyString ?: "No response from server"
                val replyCode = ftpClient.replyCode
                val errorMsg = "FTP upload rejected (Store failed). Code: $replyCode. Server response: $replyString"
                insertTransferLog(
                    TransferLogEntity(
                        ipAddress = ip,
                        titleId = titleId,
                        fileName = fileName,
                        destinationPath = remotePath,
                        sizeBytes = totalBytes,
                        status = "FAILED",
                        errorMessage = errorMsg
                    )
                )
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.localizedMessage ?: "Unknown FTP Network Error"
            Log.e("Repository", "FTP Error", e)
            insertTransferLog(
                TransferLogEntity(
                    ipAddress = ip,
                    titleId = titleId,
                    fileName = fileName,
                    destinationPath = remotePath,
                    sizeBytes = totalBytes,
                    status = "FAILED",
                    errorMessage = errorMsg
                )
            )
            Result.failure(e)
        } finally {
            try {
                progressStream?.close()
                inputStream?.close()
            } catch (_: Exception) {}
            try {
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            } catch (_: Exception) {}
        }
    }
}
