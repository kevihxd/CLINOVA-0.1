package com.clinova.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarNotificacionRechazo(String destinatario, String tipoDocumento, String motivo, String fechaLimite) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("ACCIÓN REQUERIDA: Documento Rechazado - CLINOVA");

        String cuerpoMensaje = String.format(
                "Hola,\n\n" +
                        "Desde el departamento de Talento Humano te informamos que tu documento clasificado como '%s' ha sido RECHAZADO.\n\n" +
                        "Motivo del rechazo indicado por el auditor:\n" +
                        "\"%s\"\n\n" +
                        "Por favor, ingresa a tu perfil en CLINOVA, corrige las observaciones y vuelve a subir el documento actualizado a más tardar el: %s.\n\n" +
                        "Saludos cordiales,\n" +
                        "Equipo de Talento Humano - CLINOVA.",
                tipoDocumento, motivo, fechaLimite
        );

        mensaje.setText(cuerpoMensaje);
        mailSender.send(mensaje);
    }

    public void sendEmailWithAttachment(List<String> to, String subject, String text, MultipartFile attachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(text, false);

            if (attachment != null && !attachment.isEmpty()) {
                helper.addAttachment(attachment.getOriginalFilename() != null ? attachment.getOriginalFilename() : "adjunto",
                        new ByteArrayResource(attachment.getBytes()));
            }

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo electrónico: " + e.getMessage());
        }
    }
    
    public void sendEmailWithFile(List<String> to, String subject, String text, File file) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(text, false);

            if (file != null && file.exists()) {
                helper.addAttachment(file.getName(), file);
            }

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo electrónico: " + e.getMessage());
        }
    }
}