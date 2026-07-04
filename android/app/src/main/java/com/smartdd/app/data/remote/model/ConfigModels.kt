package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class ConfigResponse(val config: UserConfigDTO)
data class UserConfigDTO(
    val id: String? = null, val userId: String? = null,
    @SerializedName("defaultMode") val defaultMode: String,
    @SerializedName("chatEnabled") val chatEnabled: Boolean,
    @SerializedName("audioEnabled") val audioEnabled: Boolean,
    @SerializedName("videoEnabled") val videoEnabled: Boolean,
    @SerializedName("timeoutSeconds") val timeoutSeconds: Int? = null
)
data class UpdateConfigRequest(
    @SerializedName("defaultMode") val defaultMode: String? = null,
    @SerializedName("chatEnabled") val chatEnabled: Boolean? = null,
    @SerializedName("audioEnabled") val audioEnabled: Boolean? = null,
    @SerializedName("videoEnabled") val videoEnabled: Boolean? = null,
    @SerializedName("timeoutSeconds") val timeoutSeconds: Int? = null
)
