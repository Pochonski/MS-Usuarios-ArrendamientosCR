package com.arrendamientos.usuarios.infrastructure.email;

import com.arrendamientos.usuarios.domain.port.out.EmailSenderPort;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.SyncPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter de Azure Communication Services para envío real de emails.
 *
 * Solo se activa cuando {@code app.email.provider=azure-comm} (producción).
 * En dev/test el {@link LoggingEmailSenderAdapter} sigue activo.
 *
 * Configuración requerida (via env vars o application*.yml):
 *   - app.email.connection-string → endpoint=...;accesskey=...
 *   - app.email.from-address      → sender validado en Azure (ej: DoNotReply@xxxxx.azurecomm.net)
 *   - app.email.from-display-name → nombre del remitente (opcional)
 *
 * @see <a href="https://learn.microsoft.com/en-us/azure/communication-services/concepts/email/email-overview">ACS Email overview</a>
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "azure-comm")
public class AzureCommunicationEmailAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(AzureCommunicationEmailAdapter.class);

    private final EmailClient client;
    private final String fromAddress;
    private final String fromDisplayName;

    public AzureCommunicationEmailAdapter(
            @Value("${app.email.connection-string:}") String connectionString,
            @Value("${app.email.from-address}") String fromAddress,
            @Value("${app.email.from-display-name:}") String fromDisplayName) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException(
                "app.email.connection-string es requerido cuando provider=azure-comm. " +
                "Configurar AZURE_COMM_CONNECTION_STRING en App Service o en application*.yml."
            );
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("app.email.from-address es requerido");
        }
        this.client = new EmailClientBuilder().connectionString(connectionString).buildClient();
        this.fromAddress = fromAddress;
        this.fromDisplayName = (fromDisplayName == null || fromDisplayName.isBlank()) ? null : fromDisplayName;
        log.info("AzureCommunicationEmailAdapter inicializado. from={} displayName={}", fromAddress, fromDisplayName);
    }

    @Override
    public void enviarVerificacion(String correo, String nombre, String verificationUrl) {
        String subject = "Verificá tu cuenta en Plataforma de Arrendamientos CR";
        String htmlBody = """
                <html>
                  <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h1 style="color: #2563eb;">¡Hola %s!</h1>
                    <p>Gracias por registrarte en <strong>Plataforma de Arrendamientos CR</strong>.</p>
                    <p>Hacé click en el siguiente botón para verificar tu correo electrónico:</p>
                    <p style="text-align: center; margin: 30px 0;">
                      <a href="%s" style="background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">
                        Verificar mi cuenta
                      </a>
                    </p>
                    <p style="color: #6b7280; font-size: 14px;">Si el botón no funciona, copiá este link en tu navegador:</p>
                    <p style="word-break: break-all; color: #6b7280; font-size: 12px;">%s</p>
                    <p style="color: #6b7280; font-size: 12px; margin-top: 30px;">Este link expira en 24 horas.</p>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 30px 0;">
                    <p style="color: #9ca3af; font-size: 11px;">Si no creaste esta cuenta, podés ignorar este email.</p>
                  </body>
                </html>
                """.formatted(escapeHtml(nombre), verificationUrl, verificationUrl);

        String textBody = """
                Hola %s,

                Gracias por registrarte en Plataforma de Arrendamientos CR.
                Verificá tu cuenta haciendo click en este link:

                %s

                (El link expira en 24 horas. Si no creaste esta cuenta, ignora este email.)
                """.formatted(nombre, verificationUrl);

        try {
            // El SDK de ACS NO soporta setFromDisplayName — el display name va
            // en el from-address formato "Display Name <email>" o se setea via
            // MailFrom del dominio en Azure Portal.
            // Por simplicidad, dejamos el from-address plano.
            EmailMessage message = new EmailMessage()
                    .setSenderAddress(fromAddress)
                    .setToRecipients(correo)
                    .setSubject(subject)
                    .setBodyHtml(htmlBody)
                    .setBodyPlainText(textBody);

            SyncPoller<EmailSendResult, EmailSendResult> poller = client.beginSend(message);
            EmailSendResult result = poller.getFinalResult();
            EmailSendStatus status = result.getStatus();

            if (status == EmailSendStatus.NOT_STARTED || status == EmailSendStatus.RUNNING || status == EmailSendStatus.SUCCEEDED) {
                log.info("Email de verificación enviado a {} (status={}, messageId={})",
                        correo, status, result.getId());
            } else {
                log.error("Email de verificación terminó con status={} para {} (id={})",
                        status, correo, result.getId());
                throw new RuntimeException("ACS rechazó el envío de email: status=" + status);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falló el envío de email de verificación a {}: {}", correo, e.getMessage(), e);
            throw new RuntimeException("Error enviando email de verificación", e);
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
