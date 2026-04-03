package com.tropig.backend.common.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.stereotype.Component

/**
 * JPA AttributeConverter for AES-256-GCM encryption.
 * Transparently encrypts on write, decrypts on read.
 */
@Converter
class EncryptedStringConverter : AttributeConverter<String?, String?> {

    companion object {
        lateinit var encryptionService: EncryptionService
    }

    override fun convertToDatabaseColumn(attribute: String?): String? =
        attribute?.let { encryptionService.encrypt(it) }

    override fun convertToEntityAttribute(dbData: String?): String? =
        dbData?.let { encryptionService.decrypt(it) }
}

/**
 * Initializes the converter with the Spring-managed EncryptionService bean.
 */
@Component
class EncryptedStringConverterInitializer(encryptionService: EncryptionService) {
    init {
        EncryptedStringConverter.encryptionService = encryptionService
    }
}
