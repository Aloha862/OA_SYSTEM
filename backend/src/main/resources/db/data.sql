USE oa_management;

INSERT IGNORE INTO sys_department (id, name, parent_id, leader_id, approver_id, sort_order, status)
VALUES
    (1, '总经办', 0, 1, 1, 1, 1),
    (2, '研发部', 1, 2, 2, 2, 1),
    (3, '行政部', 1, 1, 1, 3, 1);

INSERT IGNORE INTO sys_user
(id, username, password, real_name, gender, phone, email, avatar, role, status, department_id, position, hire_date, is_approver)
VALUES
    (1, 'admin', '$2b$12$CI8fqtXuxI/sh09DaG.hQeGlb/Mb9dX2vSZ.ooVBnav/8Bd9hL6o2', '系统管理员', 0, '13800000001', 'admin@example.com', '/default-avatar.png', 'ADMIN', 1, 1, '管理员', '2024-01-01', 1),
    (2, 'employee', '$2b$12$CI8fqtXuxI/sh09DaG.hQeGlb/Mb9dX2vSZ.ooVBnav/8Bd9hL6o2', '员工张三', 1, '13800000002', 'employee@example.com', '/default-avatar.png', 'EMPLOYEE', 1, 2, 'Java工程师', '2024-03-01', 1),
    (3, 'lisi', '$2b$12$CI8fqtXuxI/sh09DaG.hQeGlb/Mb9dX2vSZ.ooVBnav/8Bd9hL6o2', '员工李四', 2, '13800000003', 'lisi@example.com', '/default-avatar.png', 'EMPLOYEE', 1, 2, '产品经理', '2024-04-01', 0);

INSERT IGNORE INTO sys_dict_type (id, type_code, type_name, remark, status)
VALUES
    (1, 'user_status', '用户状态', NULL, 1),
    (2, 'user_role', '用户角色', NULL, 1),
    (3, 'department_status', '部门状态', NULL, 1),
    (4, 'approval_type', '审批类型', NULL, 1),
    (5, 'approval_status', '审批状态', NULL, 1),
    (6, 'schedule_type', '日程类型', NULL, 1),
    (7, 'schedule_reminder', '日程提醒方式', NULL, 1),
    (8, 'news_category', '新闻分类', NULL, 1),
    (9, 'news_status', '新闻状态', NULL, 1),
    (10, 'notification_type', '通知类型', NULL, 1),
    (11, 'ai_function_type', 'AI功能类型', NULL, 1);

INSERT IGNORE INTO sys_dict_data (type_code, dict_label, dict_value, sort_order, status)
VALUES
    ('user_status', '禁用', '0', 0, 1),
    ('user_status', '启用', '1', 1, 1),
    ('user_role', '管理员', 'ADMIN', 1, 1),
    ('user_role', '员工', 'EMPLOYEE', 2, 1),
    ('department_status', '禁用', '0', 0, 1),
    ('department_status', '启用', '1', 1, 1),
    ('approval_type', '请假', 'LEAVE', 1, 1),
    ('approval_type', '报销', 'REIMBURSEMENT', 2, 1),
    ('approval_type', '加班', 'OVERTIME', 3, 1),
    ('approval_type', '出差', 'TRAVEL', 4, 1),
    ('approval_status', '草稿', 'DRAFT', 1, 1),
    ('approval_status', '待审批', 'PENDING', 2, 1),
    ('approval_status', '已通过', 'APPROVED', 3, 1),
    ('approval_status', '已驳回', 'REJECTED', 4, 1),
    ('approval_status', '已撤回', 'WITHDRAWN', 5, 1),
    ('schedule_type', '个人日程', 'PERSONAL', 1, 1),
    ('schedule_type', '会议日程', 'MEETING', 2, 1),
    ('schedule_reminder', '提前5分钟', '5', 1, 1),
    ('schedule_reminder', '提前15分钟', '15', 2, 1),
    ('schedule_reminder', '提前30分钟', '30', 3, 1),
    ('schedule_reminder', '提前1小时', '60', 4, 1),
    ('news_category', '公司动态', 'company', 1, 1),
    ('news_category', '制度公告', 'policy', 2, 1),
    ('news_category', '团队文化', 'culture', 3, 1),
    ('news_status', '草稿', 'DRAFT', 1, 1),
    ('news_status', '已发布', 'PUBLISHED', 2, 1),
    ('news_status', '已下架', 'OFFLINE', 3, 1),
    ('notification_type', '审批', 'APPROVAL', 1, 1),
    ('notification_type', '日程', 'SCHEDULE', 2, 1),
    ('notification_type', '新闻', 'NEWS', 3, 1),
    ('notification_type', '系统', 'SYSTEM', 4, 1),
    ('ai_function_type', '审批摘要', 'APPROVAL_SUMMARY', 1, 1),
    ('ai_function_type', '审批风险', 'APPROVAL_RISK', 2, 1),
    ('ai_function_type', '新闻生成', 'NEWS_GENERATE', 3, 1),
    ('ai_function_type', '新闻润色', 'NEWS_POLISH', 4, 1),
    ('ai_function_type', '日程解析', 'SCHEDULE_PARSE', 5, 1),
    ('ai_function_type', '智能问答', 'QA', 6, 1);

INSERT IGNORE INTO oa_news (id, title, summary, content, category, cover_image, status, is_top, publisher_id, published_at)
VALUES (1, '欢迎使用企业 OA 管理系统', '系统初始化新闻', '这里是企业 OA 管理系统的第一条新闻，可在后台继续编辑。', 'company', '/files/2026/06/seed-cover-01.jpg', 'PUBLISHED', 1, 1, NOW());
