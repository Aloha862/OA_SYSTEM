package com.example.oa.module.notification.service.impl;

import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.config.MailNotificationProperties;
import com.example.oa.module.notification.service.MailService;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final UserMapper userMapper;
    private final MailNotificationProperties mailNotificationProperties;

    @Value("${spring.mail.username:}")
    private String sender;

    @Async
    @Retryable(retryFor = MailException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Override
    public void sendNotificationMail(NotificationMessage message) {
        User receiver = userMapper.selectById(message.getReceiverId());
        String recipient = StringUtils.hasText(mailNotificationProperties.getNotificationRecipient())
                ? mailNotificationProperties.getNotificationRecipient()
                : receiver == null ? null : receiver.getEmail();
        if (!StringUtils.hasText(recipient)) {
            log.info("用户未配置邮箱，跳过邮件通知: receiverId={}", message.getReceiverId());
            return;
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        if (StringUtils.hasText(sender)) {
            mail.setFrom(sender);
        }
        mail.setTo(recipient);
        mail.setSubject("[OA通知] " + message.getTitle());
        mail.setText(buildText(message, receiver));
        mailSender.send(mail);
        log.info("邮件通知发送成功: receiverId={}, eventId={}", message.getReceiverId(), message.getEventId());
    }

    @Recover
    public void recover(MailException exception, NotificationMessage message) {
        log.error("邮件通知重试耗尽: receiverId={}, eventId={}, title={}",
                message.getReceiverId(), message.getEventId(), message.getTitle(), exception);
    }

    private String buildText(NotificationMessage message, User receiver) {
        String receiverText = receiver == null
                ? "未知用户(" + message.getReceiverId() + ")"
                : receiver.getRealName() + " / " + receiver.getUsername() + " / " + nullToDash(receiver.getEmail());
        return """
                企业 OA 通知

                标题：%s
                类型：%s
                原接收人：%s
                业务类型：%s
                业务 ID：%s

                内容：
                %s
                """.formatted(
                message.getTitle(),
                nullToDash(message.getType()),
                receiverText,
                nullToDash(message.getBusinessType()),
                message.getBusinessId() == null ? "-" : String.valueOf(message.getBusinessId()),
                nullToDash(message.getContent()));
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
