package org.example.greenybackend.modules.notification.impl;

import org.example.greenybackend.modules.notification.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String backendBaseUrl;

    public EmailServiceImpl(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${greeny.auth.backend-base-url:http://localhost:8080}") String backendBaseUrl
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.backendBaseUrl = backendBaseUrl;
    }

    @Override
    public void sendRegisterVerification(String toEmail, String token) {
        String verifyUrl = backendBaseUrl + "/api/auth/verify-email?token=" + token;
        send(toEmail, "Xac thuc email dang ky Greeny",
                "Vui long xac thuc email de hoan tat dang ky: " + verifyUrl);
    }

    @Override
    public void sendDeleteAccountVerification(String toEmail, String token) {
        String verifyUrl = backendBaseUrl + "/api/auth/account/delete-confirm?token=" + token;
        send(toEmail, "Xac thuc xoa tai khoan Greeny",
                "Neu ban muon xoa tai khoan, vui long xac thuc tai: " + verifyUrl);
    }

    private void send(String toEmail, String subject, String content) {
        if (mailSender == null) {
            System.out.println("[Greeny email] To: " + toEmail);
            System.out.println("[Greeny email] Subject: " + subject);
            System.out.println("[Greeny email] Content: " + content);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            System.out.println("[Greeny email] Khong gui duoc SMTP.");
            System.out.println("[Greeny email] To: " + toEmail);
            System.out.println("[Greeny email] Subject: " + subject);
            System.out.println("[Greeny email] Content: " + content);
        }
    }
}
