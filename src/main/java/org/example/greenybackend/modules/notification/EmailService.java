package org.example.greenybackend.modules.notification;

public interface EmailService {

    void sendRegisterVerification(String toEmail, String token);

    void sendDeleteAccountVerification(String toEmail, String token);

}
