package com.tropig.backend.common.util

import com.tropig.backend.common.enums.MailType
import com.tropig.backend.config.MailProperties
import jakarta.mail.internet.InternetAddress
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailUtil(
    private val mailSender: JavaMailSender,
    private val properties: MailProperties,
) {

    fun send(to: String, mailType: MailType, html: String) {
        val message = mailSender.createMimeMessage()
        val name = mailType.name.lowercase()
        val subject = properties.subjects[name] ?: return

        val helper = MimeMessageHelper(message, true, "UTF-8")

        requireNotNull(properties.sender)

        helper.setFrom(
            InternetAddress(
                properties.sender.address,
                properties.sender.name
            )
        )
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(html, true)

        mailSender.send(message)
    }
}