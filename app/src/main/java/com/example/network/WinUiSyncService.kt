package com.example.network

import android.content.Context
import com.example.data.SyncedPhoto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

interface WinUiPhotosApi {
    @GET("api/status")
    suspend fun getStatus(): HostStatusResponse

    @GET("api/photos")
    suspend fun getPhotos(): List<HostPhotoResponse>
}

class WinUiSyncService(private val context: Context) {
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Helper to generate a Retrofit client on-the-fly for a specific IP/Port
    fun getApi(ip: String, port: String): WinUiPhotosApi {
        val baseUrl = sanitizeBaseUrl(ip, port)
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WinUiPhotosApi::class.java)
    }

    // Ping/test connection to the given host details
    suspend fun testConnection(ip: String, port: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val api = getApi(ip, port)
                val response = api.getStatus()
                response.status == "ok" && response.app == "WinUI 3 Photos App"
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Sync metadata from host PC and return SyncedPhoto objects
    suspend fun fetchPhotosFromPC(ip: String, port: String): List<SyncedPhoto> {
        return withContext(Dispatchers.IO) {
            try {
                val api = getApi(ip, port)
                val photosResponse = api.getPhotos()
                val baseUrl = sanitizeBaseUrl(ip, port)
                
                photosResponse.map { resp ->
                    SyncedPhoto(
                        id = resp.id,
                        filename = resp.filename,
                        timestamp = resp.timestamp,
                        size = resp.size,
                        description = resp.description,
                        remoteUrl = "${baseUrl}api/photos/${resp.id}/full",
                        localUri = null,
                        isDownloaded = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Helper to construct thumbnail URL
    fun getThumbnailUrl(ip: String, port: String, id: String): String {
        return "${sanitizeBaseUrl(ip, port)}api/photos/$id/thumbnail"
    }

    // Download full size photo from host PC to private memory
    suspend fun downloadPhoto(
        ip: String,
        port: String,
        id: String,
        filename: String,
        onProgress: (Float) -> Unit
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = sanitizeBaseUrl(ip, port)
                val url = "${baseUrl}api/photos/$id/full"
                
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) return@withContext null
                
                val body = response.body ?: return@withContext null
                val contentLength = body.contentLength()
                
                // Create private storage directory
                val targetDir = File(context.filesDir, "synced_photos")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                
                val targetFile = File(targetDir, "photo_${id}_$filename")
                val input: InputStream = body.byteStream()
                val output = FileOutputStream(targetFile)
                
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        onProgress(totalBytesRead.toFloat() / contentLength)
                    }
                }
                
                output.flush()
                output.close()
                input.close()
                
                targetFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun sanitizeBaseUrl(ip: String, port: String): String {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val cleanPort = port.trim()
        val portSuffix = if (cleanPort.isNotEmpty()) ":$cleanPort" else ""
        return "http://$cleanIp$portSuffix/"
    }
}
