CREATE DATABASE IF NOT EXISTS oa_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE oa_management;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密',
    real_name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT '/default-avatar.png' COMMENT '头像',
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色：ADMIN/EMPLOYEE',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    department_id BIGINT COMMENT '所属部门ID',
    position VARCHAR(100) COMMENT '岗位',
    hire_date DATE COMMENT '入职日期',
    is_approver TINYINT DEFAULT 0 COMMENT '是否审批人：0否，1是',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_username (username),
    INDEX idx_department_id (department_id),
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_is_approver (is_approver)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '上级部门ID',
    leader_id BIGINT COMMENT '部门负责人ID',
    approver_id BIGINT COMMENT '部门默认审批人ID',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id),
    INDEX idx_leader_id (leader_id),
    INDEX idx_approver_id (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS oa_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批ID',
    approval_no VARCHAR(50) NOT NULL UNIQUE COMMENT '审批编号',
    title VARCHAR(200) NOT NULL COMMENT '审批标题',
    type VARCHAR(30) NOT NULL COMMENT '审批类型：LEAVE/REIMBURSEMENT/OVERTIME/TRAVEL',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING/APPROVED/REJECTED/WITHDRAWN',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    department_id BIGINT COMMENT '申请人部门ID',
    approver_id BIGINT COMMENT '审批人ID',
    reason TEXT COMMENT '申请原因',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    amount DECIMAL(10,2) COMMENT '报销金额',
    destination VARCHAR(255) COMMENT '出差目的地',
    form_data JSON COMMENT '扩展表单数据',
    ai_summary TEXT COMMENT 'AI摘要',
    ai_risk_level VARCHAR(20) COMMENT 'AI风险等级：LOW/MEDIUM/HIGH',
    ai_risk_suggestion TEXT COMMENT 'AI风险建议',
    submitted_at DATETIME COMMENT '提交时间',
    approved_at DATETIME COMMENT '审批时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_approval_no (approval_no),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表';

CREATE TABLE IF NOT EXISTS oa_approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审批记录ID',
    approval_id BIGINT NOT NULL COMMENT '审批ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    action VARCHAR(30) NOT NULL COMMENT '操作：SUBMIT/APPROVE/REJECT/WITHDRAW',
    comment VARCHAR(500) COMMENT '审批意见',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_approval_id (approval_id),
    INDEX idx_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

CREATE TABLE IF NOT EXISTS oa_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日程ID',
    title VARCHAR(200) NOT NULL COMMENT '日程标题',
    content TEXT COMMENT '日程内容',
    type VARCHAR(30) NOT NULL DEFAULT 'PERSONAL' COMMENT '类型：PERSONAL/MEETING',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    location VARCHAR(255) COMMENT '地点',
    reminder_minutes INT DEFAULT 15 COMMENT '提前提醒分钟数',
    reminder_time DATETIME COMMENT '实际提醒时间',
    reminder_status TINYINT DEFAULT 0 COMMENT '提醒状态：0未提醒，1已提醒',
    status VARCHAR(30) DEFAULT 'NORMAL' COMMENT '状态：NORMAL/CANCELLED/FINISHED',
    ai_origin_text TEXT COMMENT 'AI自然语言原文',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_creator_id (creator_id),
    INDEX idx_start_time (start_time),
    INDEX idx_reminder_time (reminder_time),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日程表';

CREATE TABLE IF NOT EXISTS oa_schedule_participant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '参与人ID',
    schedule_id BIGINT NOT NULL COMMENT '日程ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status VARCHAR(30) DEFAULT 'PENDING' COMMENT '状态：PENDING/ACCEPTED/REJECTED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_schedule_user (schedule_id, user_id),
    INDEX idx_schedule_id (schedule_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日程参与人表';

CREATE TABLE IF NOT EXISTS oa_news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '新闻ID',
    title VARCHAR(200) NOT NULL COMMENT '新闻标题',
    summary VARCHAR(500) COMMENT '摘要',
    content LONGTEXT NOT NULL COMMENT '正文',
    category VARCHAR(50) COMMENT '分类',
    cover_image VARCHAR(255) COMMENT '封面图',
    status VARCHAR(30) DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶',
    view_count INT DEFAULT 0 COMMENT '阅读量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    favorite_count INT DEFAULT 0 COMMENT '收藏数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    publisher_id BIGINT COMMENT '发布人ID',
    published_at DATETIME COMMENT '发布时间',
    ai_generated TINYINT DEFAULT 0 COMMENT '是否AI生成',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_publisher_id (publisher_id),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻表';

CREATE TABLE IF NOT EXISTS oa_news_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    news_id BIGINT NOT NULL COMMENT '新闻ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    parent_id BIGINT DEFAULT 0 COMMENT '父评论ID',
    status VARCHAR(30) DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/DELETED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_news_id (news_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻评论表';

CREATE TABLE IF NOT EXISTS oa_news_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    news_id BIGINT NOT NULL COMMENT '新闻ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_news_user (news_id, user_id),
    INDEX idx_news_id (news_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻点赞表';

CREATE TABLE IF NOT EXISTS oa_news_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    news_id BIGINT NOT NULL COMMENT '新闻ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_news_user (news_id, user_id),
    INDEX idx_news_id (news_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻收藏表';

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典类型ID',
    type_code VARCHAR(100) NOT NULL UNIQUE COMMENT '字典类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '字典类型名称',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID',
    type_code VARCHAR(100) NOT NULL COMMENT '字典类型编码',
    dict_label VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_type_value_label (type_code, dict_value, dict_label),
    INDEX idx_type_code (type_code),
    INDEX idx_dict_value (dict_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_url VARCHAR(500) COMMENT '访问URL',
    file_size BIGINT COMMENT '文件大小',
    file_type VARCHAR(100) COMMENT '文件类型',
    extension VARCHAR(20) COMMENT '扩展名',
    business_type VARCHAR(50) COMMENT '业务类型：AVATAR/APPROVAL/SCHEDULE/NEWS',
    business_id BIGINT COMMENT '业务ID',
    uploader_id BIGINT COMMENT '上传人ID',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_business (business_type, business_id),
    INDEX idx_uploader_id (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    event_id VARCHAR(64) COMMENT '消息幂等ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    sender_id BIGINT COMMENT '发送人ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) COMMENT '通知内容',
    type VARCHAR(50) NOT NULL COMMENT '通知类型',
    business_type VARCHAR(50) COMMENT '业务类型',
    business_id BIGINT COMMENT '业务ID',
    read_status TINYINT DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
    read_time DATETIME COMMENT '阅读时间',
    pushed TINYINT DEFAULT 0 COMMENT '是否已WebSocket推送',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_receiver_id (receiver_id),
    UNIQUE KEY uk_event_receiver (event_id, receiver_id),
    INDEX idx_read_status (read_status),
    INDEX idx_type (type),
    INDEX idx_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信通知表';

CREATE TABLE IF NOT EXISTS sys_ai_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI日志ID',
    user_id BIGINT NOT NULL COMMENT '调用用户ID',
    function_type VARCHAR(50) NOT NULL COMMENT '功能类型',
    provider VARCHAR(30) DEFAULT 'mock' COMMENT '模型提供商：mock/tongyi',
    model_name VARCHAR(100) COMMENT '模型名称',
    prompt TEXT COMMENT '提示词',
    request_content LONGTEXT COMMENT '请求内容',
    response_content LONGTEXT COMMENT '响应内容',
    success TINYINT DEFAULT 1 COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',
    cost_time_ms BIGINT COMMENT '耗时毫秒',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_function_type (function_type),
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';
