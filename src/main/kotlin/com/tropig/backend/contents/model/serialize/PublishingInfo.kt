package com.tropig.backend.contents.model.serialize

import com.tropig.backend.contents.enums.PublishingType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PublishingInfo(var type: PublishingType, var path: String?)

/**
 * List<PublishingInfo>를 JSON 문자열로 변환
 */
fun List<PublishingInfo>.toJson(): String = Json.encodeToString(this)

/**
 * JSON 문자열을 List<PublishingInfo>로 변환
 */
fun String.toPublishingInfoList(): List<PublishingInfo> = Json.decodeFromString(this)
