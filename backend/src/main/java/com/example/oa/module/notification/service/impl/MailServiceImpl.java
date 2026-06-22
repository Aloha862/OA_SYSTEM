package com.example.oa.module.notification.service.impl;

import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.config.MailNotificationProperties;
import com.example.oa.module.notification.service.MailService;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    @Async
    @Override
    public void sendNotificationMail(NotificationMessage message) {
        try {
            String recipient = mailNotificationProperties.getNotificationRecipient();
            if (!StringUtils.hasText(recipient)) {
                return;
            }
            User receiver = userMapper.selectById(message.getReceiverId());
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(recipient);
            mail.setSubject("[OA通知] " + message.getTitle());
            mail.setText(buildText(message, receiver));
            mailSender.send(mail);
        } catch (Exception e) {
            log.warn("邮件发送失败，不影响主流程: receiverId={}, title={}", message.getReceiverId(), message.getTitle(), e);
        }
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
