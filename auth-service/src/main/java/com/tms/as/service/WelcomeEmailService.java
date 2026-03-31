package com.tms.as.service;

import com.tms.as.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class WelcomeEmailService {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:no-reply@tms.local}")
    private String fromEmail;

    @Value("${app.mail.welcome-subject:Welcome to TimeSheet and Leave Management System}")
    private String welcomeSubject;

    public WelcomeEmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendWelcomeEmail(User user) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("Skipping welcome email for userId={} because mail sender is not configured", user.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(welcomeSubject);
            helper.setText(buildWelcomeBody(user), false);
            mailSender.send(message);
            log.info("Welcome email sent successfully to userId={} email={}", user.getId(), user.getEmail());
        } catch (MailException | jakarta.mail.MessagingException ex) {
            log.error("Failed to send welcome email to userId={} email={}", user.getId(), user.getEmail(), ex);
        }
    }

    private String buildWelcomeBody(User user) {
        return "Hello " + user.getFullName() + ",\n\n"
                + "Welcome to the TimeSheet and Leave Management System.\n\n"
                + "Your account has been created successfully.\n"
                + "Employee Code: " + user.getEmployeeCode() + "\n"
                + "Registered Email: " + user.getEmail() + "\n\n"
                + "You can now log in and start using the platform.\n\n"
                + "Regards,\n"
                + "TMS Team";
    }
}
