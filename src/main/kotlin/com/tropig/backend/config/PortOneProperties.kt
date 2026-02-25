package com.tropig.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "portone")
data class PortOneProperties(val storeId: String = "", val channels: ChannelProperties = ChannelProperties()) {
    data class ChannelProperties(val kcp: String = "", val kakaoPay: String = "")
}
