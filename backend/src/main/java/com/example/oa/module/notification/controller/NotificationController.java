package com.example.oa.module.notification.controller;

import com.example.oa.common.dto.IdListRequest;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.notification.dto.NotificationQueryRequest;
import com.example.oa.module.notification.dto.SystemNotificationRequest;
import com.example.oa.module.notification.entity.Notification;
import com.example.oa.module.notification.entity.NotificationOutbox;
import com.example.oa.module.notification.entity.MailOutbox;
import com.example.oa.module.notification.service.NotificationOutboxService;
import com.example.oa.module.notification.service.MailService;
import com.example.oa.module.notification.service.NotificationService;
import com.example.oa.module.notification.websocket.WebSocketTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final WebSocketTicketService webSocketTicketService;
    private final NotificationOutboxService notificationOutboxService;
    private final MailService mailService;

    @PostMapping("/ws-ticket")
    public Result<Map<String, String>> webSocketTicket() {
        return Result.success(Map.of("ticket", webSocketTicketService.issue()));
    }

    @GetMapping("/page")
    public Result<PageResult<Notification>> page(NotificationQueryRequest request) {
        return Result.success(notificationService.pageMine(request));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success(null);
    }

    @PutMapping("/read-batch")
    public Result<Void> readBatch(@Valid @RequestBody IdListRequest request) {
        notificationService.markReadBatch(request.getIds());
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.deleteMine(id);
        return Result.success(null);
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> system(@Valid @RequestBody SystemNotificationRequest request) {
        notificationService.sendSystem(request);
        return Result.success(null);
    }

    @GetMapping("/outbox/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<NotificationOutbox>> outboxPage(String status,
                                                              @RequestParam(defaultValue = "1") long current,
                                                              @RequestParam(defaultValue = "20") long size) {
        return Result.success(notificationOutboxService.page(status, current, size));
    }

    @PostMapping("/outbox/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> retryOutbox(@PathVariable Long id) {
        notificationOutboxService.retry(id);
        return Result.success(null);
    }

    @GetMapping("/mail-outbox/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<MailOutbox>> mailOutboxPage(String status,
                                                         @RequestParam(defaultValue = "1") long current,
                                                         @RequestParam(defaultValue = "20") long size) {
        return Result.success(mailService.pageOutbox(status, current, size));
    }

    @PostMapping("/mail-outbox/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> retryMailOutbox(@PathVariable Long id) {
        mailService.retryOutbox(id);
        return Result.success(null);
    }
}
