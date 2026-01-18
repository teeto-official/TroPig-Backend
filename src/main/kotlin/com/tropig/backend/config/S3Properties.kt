package com.tropig.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val bucket: String,
    val region: String = "ap-northeast-2",
    val accessKey: String,
    val secretKey: String
)
