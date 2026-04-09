package com.clinova.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
}