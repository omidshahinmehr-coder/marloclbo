package ir.lbo.locationsms

import java.io.File
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

data class EmailResult(val success: Boolean, val errorMessage: String? = null)

object EmailSender {

    /**
     * Sends an email with one or more file attachments over SMTP.
     * Must be called from a background thread/coroutine — this performs
     * blocking network I/O.
     *
     * Old Android versions (e.g. Android 5.x) often don't enable TLS 1.2 by
     * default in their SSL socket configuration, which makes the handshake
     * with Gmail's SMTP server hang or fail silently. We explicitly force
     * TLS 1.2 and set reasonable timeouts so a failure is reported quickly
     * instead of appearing as "nothing happens".
     */
    fun sendLogEmail(
        smtpHost: String,
        smtpPort: String,
        senderEmail: String,
        senderPassword: String,
        recipientEmail: String,
        subject: String,
        bodyText: String,
        attachmentFiles: List<File>
    ): EmailResult {
        return try {
            val props = Properties().apply {
                put("mail.smtp.host", smtpHost)
                put("mail.smtp.port", smtpPort)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.ssl.trust", smtpHost)
                // Force TLS 1.2 — needed for reliable SMTP on Android 5/6 devices
                put("mail.smtp.ssl.protocols", "TLSv1.2")
                put("mail.smtp.connectiontimeout", "20000")
                put("mail.smtp.timeout", "20000")
                put("mail.smtp.writetimeout", "20000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                setSubject(subject)
            }

            val multipart = MimeMultipart()

            val textPart = MimeBodyPart().apply { setText(bodyText) }
            multipart.addBodyPart(textPart)

            attachmentFiles.forEach { file ->
                if (file.exists()) {
                    val attachmentPart = MimeBodyPart()
                    attachmentPart.attachFile(file)
                    multipart.addBodyPart(attachmentPart)
                }
            }

            message.setContent(multipart)
            Transport.send(message)
            EmailResult(true)
        } catch (e: Exception) {
            EmailResult(false, e.message ?: e.javaClass.simpleName)
        }
    }
}
