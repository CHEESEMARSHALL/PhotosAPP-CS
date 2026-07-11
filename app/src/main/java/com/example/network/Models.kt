package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HostStatusResponse(
    @Json(name = "status") val status: String,
    @Json(name = "app") val app: String
)

@JsonClass(generateAdapter = true)
data class HostPhotoResponse(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "size") val size: Long,
    @Json(name = "description") val description: String? = null
)
