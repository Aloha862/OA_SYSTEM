package com.example.oa.module.notification;

import com.example.oa.module.notification.config.MailNotificationProperties;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.entity.MailOutbox;
import com.example.oa.module.notification.mapper.MailOutboxMapper;
import com.example.oa.module.notification.service.impl.MailServiceImpl;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MailOutboxTest {
    @Test
    void missingRecipientIsPersistedAsSkippedInsteadOfSilentlyLost() {
        JavaMailSender sender = mock(JavaMailSender.class);
        UserMapper users = mock(UserMapper.class);
        MailOutboxMapper outbox = mock(MailOutboxMapper.class);
        when(users.selectById(7L)).thenReturn(new User());
        MailServiceImpl service = new MailServiceImpl(sender, users, outbox, new MailNotificationProperties());
        NotificationMessage message = new NotificationMessage();
        message.setEventId("mail-event");
        message.setReceiverId(7L);
        message.setTitle("提醒");
        message.setContent("内容");

        service.sendNotificationMail(message);

        ArgumentCaptor<MailOutbox> captor = ArgumentCaptor.forClass(MailOutbox.class);
        verify(outbox).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SKIPPED");
        assertThat(captor.getValue().getLastError()).isEqualTo("用户未配置邮箱");
        verifyNoInteractions(sender);
    }
}
