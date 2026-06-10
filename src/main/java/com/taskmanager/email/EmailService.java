package com.taskmanager.email;

import com.taskmanager.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * Sends transactional emails asynchronously so request threads are never blocked by SMTP latency.
 * Templates live under {@code resources/templates/email} and are rendered with Thymeleaf.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties properties;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, AppProperties properties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    @Async
    public void sendPasswordResetCode(String to, String fullName, String code, long ttlMinutes) {
        Context context = new Context();
        context.setVariable("fullName", (fullName == null || fullName.isBlank()) ? to : fullName);
        context.setVariable("code", code);
        context.setVariable("ttlMinutes", ttlMinutes);
        String html = templateEngine.process("email/password-reset", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getMail().getFrom());
            helper.setTo(to);
            helper.setSubject("Mã đặt lại mật khẩu - Task Manager");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent password-reset code email to {}", to);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send password-reset email to {}: {}", to, ex.getMessage());
        }
    }

    /** Reminds the user about a task that has reached its remind-at time. */
    @Async
    public void sendTaskReminder(String to, String fullName, String taskTitle, String dueText) {
        Context context = new Context();
        context.setVariable("fullName", (fullName == null || fullName.isBlank()) ? to : fullName);
        context.setVariable("taskTitle", taskTitle);
        context.setVariable("dueText", dueText);
        String html = templateEngine.process("email/task-reminder", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getMail().getFrom());
            helper.setTo(to);
            helper.setSubject("Nhắc việc: " + taskTitle + " - Task Manager");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent task-reminder email to {}", to);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send task-reminder email to {}: {}", to, ex.getMessage());
        }
    }

    /** Notifies a member that a task has been assigned to them. */
    @Async
    public void sendTaskAssigned(String to, String fullName, String taskTitle, String message) {
        Context context = new Context();
        context.setVariable("fullName", (fullName == null || fullName.isBlank()) ? to : fullName);
        context.setVariable("taskTitle", taskTitle);
        context.setVariable("message", (message == null || message.isBlank())
                ? "Bạn vừa được giao một công việc mới." : message);
        String html = templateEngine.process("email/task-assigned", context);

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getMail().getFrom());
            helper.setTo(to);
            helper.setSubject("Bạn được giao việc: " + taskTitle + " - Task Manager");
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Sent task-assigned email to {}", to);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send task-assigned email to {}: {}", to, ex.getMessage());
        }
    }

    /** Invites someone to join a workspace; the link carries the accept token. */
    @Async
    public void sendWorkspaceInvitation(String to, String inviterName, String workspaceName,
                                        String roleLabel, String acceptUrl) {
        Context context = new Context();
        context.setVariable("inviterName", (inviterName == null || inviterName.isBlank()) ? "Một thành viên" : inviterName);
        context.setVariable("workspaceName", workspaceName);
        context.setVariable("roleLabel", roleLabel);
        context.setVariable("acceptUrl", acceptUrl);
        String html = templateEngine.process("email/workspace-invitation", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getMail().getFrom());
            helper.setTo(to);
            helper.setSubject("Lời mời tham gia workspace \"" + workspaceName + "\" - Task Manager");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent workspace-invitation email to {}", to);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send workspace-invitation email to {}: {}", to, ex.getMessage());
        }
    }
}
