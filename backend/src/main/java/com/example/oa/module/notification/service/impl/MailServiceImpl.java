package com.example.oa.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.module.notification.config.MailNotificationProperties;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.entity.MailOutbox;
import com.example.oa.module.notification.mapper.MailOutboxMapper;
import com.example.oa.module.notification.service.MailService;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.mapper.UserMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private static final int MAX_ATTEMPTS = 3;
    private static final int SENDING_LEASE_MINUTES = 2;
    private final JavaMailSender mailSender;
    private final UserMapper userMapper;
    private final MailOutboxMapper outboxMapper;
    private final MailNotificationProperties mailNotificationProperties;

    @Value("${spring.mail.username:}")
    private String sender;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotificationMail(NotificationMessage message) {
        User receiver = userMapper.selectById(message.getReceiverId());
        String recipient = StringUtils.hasText(mailNotificationProperties.getNotificationRecipient())
                ? mailNotificationProperties.getNotificationRecipient()
                : receiver == null ? null : receiver.getEmail();
        MailOutbox row = new MailOutbox();
        row.setEventId(message.getEventId());
        row.setReceiverId(message.getReceiverId());
        row.setRecipient(StringUtils.hasText(recipient) ? recipient.trim() : "");
        row.setSubject("[OA通知] " + message.getTitle());
        row.setHtmlContent(buildHtml(message, receiver));
        row.setRetryCount(0);
        row.setStatus(StringUtils.hasText(recipient) ? "PENDING" : "SKIPPED");
        row.setLastError(StringUtils.hasText(recipient) ? null : "用户未配置邮箱");
        try {
            outboxMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            log.info("邮件Outbox重复事件已忽略: eventId={}, receiverId={}", message.getEventId(), message.getReceiverId());
        }
    }

    @Override
    public PageResult<MailOutbox> pageOutbox(String status, long current, long size) {
        Page<MailOutbox> page = outboxMapper.selectPage(new Page<>(Math.max(current, 1), Math.max(1, Math.min(size, 100))),
                new LambdaQueryWrapper<MailOutbox>()
                        .eq(StringUtils.hasText(status), MailOutbox::getStatus, status)
                        .orderByDesc(MailOutbox::getId));
        return PageResult.of(page);
    }

    @Override
    public void retryOutbox(Long id) {
        int updated = outboxMapper.update(null, new LambdaUpdateWrapper<MailOutbox>()
                .eq(MailOutbox::getId, id)
                .in(MailOutbox::getStatus, List.of("FAILED", "FAILED_FINAL"))
                .set(MailOutbox::getStatus, "PENDING")
                .set(MailOutbox::getRetryCount, 0)
                .set(MailOutbox::getNextRetryAt, null)
                .set(MailOutbox::getLastError, null)
                .set(MailOutbox::getUpdatedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(409, "仅失败的邮件 Outbox 可以重试");
        }
    }

    @Scheduled(fixedDelayString = "${oa.mail.outbox.fixed-delay-ms:5000}")
    public void deliverDue() {
        LocalDateTime now = LocalDateTime.now();
        outboxMapper.update(null, new LambdaUpdateWrapper<MailOutbox>()
                .eq(MailOutbox::getStatus, "SENDING")
                .le(MailOutbox::getUpdatedAt, now.minusMinutes(SENDING_LEASE_MINUTES))
                .set(MailOutbox::getStatus, "FAILED")
                .set(MailOutbox::getNextRetryAt, now)
                .set(MailOutbox::getLastError, "发送租约超时，等待重新投递")
                .set(MailOutbox::getUpdatedAt, now));
        List<MailOutbox> due = outboxMapper.selectList(new LambdaQueryWrapper<MailOutbox>()
                .in(MailOutbox::getStatus, List.of("PENDING", "FAILED"))
                .and(w -> w.isNull(MailOutbox::getNextRetryAt).or().le(MailOutbox::getNextRetryAt, now))
                .orderByAsc(MailOutbox::getId)
                .last("LIMIT 20"));
        due.forEach(this::deliverOne);
    }

    private void deliverOne(MailOutbox row) {
        boolean claimed = outboxMapper.update(null, new LambdaUpdateWrapper<MailOutbox>()
                .eq(MailOutbox::getId, row.getId())
                .in(MailOutbox::getStatus, List.of("PENDING", "FAILED"))
                .set(MailOutbox::getStatus, "SENDING")
                .set(MailOutbox::getUpdatedAt, LocalDateTime.now())) == 1;
        if (!claimed) return;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            if (StringUtils.hasText(sender)) helper.setFrom(sender);
            helper.setTo(row.getRecipient());
            helper.setSubject(row.getSubject());
            helper.setText(row.getHtmlContent(), true);
            mailSender.send(mimeMessage);
            outboxMapper.update(null, new LambdaUpdateWrapper<MailOutbox>()
                    .eq(MailOutbox::getId, row.getId())
                    .set(MailOutbox::getStatus, "SENT")
                    .set(MailOutbox::getSentAt, LocalDateTime.now())
                    .set(MailOutbox::getLastError, null));
            log.info("邮件发送成功: receiverId={}, eventId={}", row.getReceiverId(), row.getEventId());
        } catch (Exception e) {
            int retries = row.getRetryCount() == null ? 1 : row.getRetryCount() + 1;
            outboxMapper.update(null, new LambdaUpdateWrapper<MailOutbox>()
                    .eq(MailOutbox::getId, row.getId())
                    .set(MailOutbox::getStatus, retries >= MAX_ATTEMPTS ? "FAILED_FINAL" : "FAILED")
                    .set(MailOutbox::getRetryCount, retries)
                    .set(MailOutbox::getNextRetryAt, LocalDateTime.now().plusSeconds(30L * (1L << Math.min(retries - 1, 4))))
                    .set(MailOutbox::getLastError, safeError(e)));
            log.warn("邮件发送失败: receiverId={}, eventId={}, retry={}", row.getReceiverId(), row.getEventId(), retries, e);
        }
    }

    private String buildHtml(NotificationMessage message, User receiver) {
        String receiverText = receiver == null ? "未知用户(" + message.getReceiverId() + ")"
                : receiver.getRealName() + " / " + receiver.getUsername();
        return """
                <!doctype html><html><body style="font-family:Arial,'Microsoft YaHei',sans-serif;color:#172033;background:#f5f7fb;padding:24px">
                <div style="max-width:640px;margin:auto;background:#fff;border-radius:14px;padding:28px;border:1px solid #e6eaf0">
                <div style="color:#245eea;font-size:13px;font-weight:700">企业 OA 通知</div>
                <h2 style="margin:10px 0 18px">%s</h2>
                <p style="line-height:1.8;white-space:pre-wrap">%s</p>
                <hr style="border:0;border-top:1px solid #edf0f4;margin:22px 0">
                <div style="font-size:12px;color:#778196">接收人：%s<br>类型：%s<br>业务：%s / %s</div>
                </div></body></html>
                """.formatted(escape(message.getTitle()), escape(message.getContent()), escape(receiverText),
                escape(message.getType()), escape(message.getBusinessType()), message.getBusinessId() == null ? "-" : message.getBusinessId());
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(StringUtils.hasText(value) ? value : "-");
    }

    private String safeError(Exception e) {
        String value = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return value.substring(0, Math.min(500, value.length()));
    }
}
