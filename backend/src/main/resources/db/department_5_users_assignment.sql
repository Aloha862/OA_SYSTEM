USE oa_management;
SET NAMES utf8mb4;

START TRANSACTION;

-- 1) Keep exactly 5 active departments for the OA demo.
--    Existing departments outside ids 1-5 are logically deleted so historical data is not physically removed.
UPDATE sys_department
SET deleted = 1, status = 0
WHERE id NOT IN (1, 2, 3, 4, 5);

INSERT INTO sys_department
    (id, name, parent_id, leader_id, approver_id, sort_order, status, deleted)
VALUES
    (1, '综合管理部', 0,
        COALESCE((SELECT id FROM sys_user WHERE username = 'admin' AND deleted = 0 LIMIT 1), 1),
        COALESCE((SELECT id FROM sys_user WHERE username = 'admin' AND deleted = 0 LIMIT 1), 1),
        1, 1, 0),
    (2, '人力行政部', 0,
        COALESCE((SELECT id FROM sys_user WHERE username = 'oa_user_02' AND deleted = 0 LIMIT 1), 1),
        COALESCE((SELECT id FROM sys_user WHERE username = 'oa_user_06' AND deleted = 0 LIMIT 1), 1),
        2, 1, 0),
    (3, '财务法务部', 0,
        COALESCE((SELECT id FROM sys_user WHERE username = 'oa_user_03' AND deleted = 0 LIMIT 1), 1),
        COALESCE((SELECT id FROM sys_user WHERE username = 'oa_user_16' AND deleted = 0 LIMIT 1), 1),
        3, 1, 0),
    (4, '产品研发部', 0,
        COALESCE((SELECT id FROM sys_user WHERE username = 'employee' AND deleted = 0 LIMIT 1), 1),
        COALESCE((SELECT id FROM sys_user WHERE username = 'employee' AND deleted = 0 LIMIT 1), 1),
        4, 1, 0),
    (5, '市场运营部', 0,
        COALESCE((SELECT id FROM sys_user WHERE username = 'oa_user_11' AND deleted = 0 LIMIT 1), 1),
        COALESCE((SELECT id FROM sys_user WHERE username = 'oa_user_11' AND deleted = 0 LIMIT 1), 1),
        5, 1, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    parent_id = VALUES(parent_id),
    leader_id = VALUES(leader_id),
    approver_id = VALUES(approver_id),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP;

-- 2) Assign every active user to one of the 5 departments.
UPDATE sys_user
SET department_id = CASE
    WHEN username = 'admin' OR role = 'ADMIN' THEN 1
    WHEN position LIKE '%HR%'
      OR position LIKE '%人事%'
      OR position LIKE '%行政%'
      OR position LIKE '%培训%'
      OR position LIKE '%采购%' THEN 2
    WHEN position LIKE '%财务%'
      OR position LIKE '%会计%'
      OR position LIKE '%法务%'
      OR position LIKE '%合规%' THEN 3
    WHEN username IN ('employee', 'lisi')
      OR position LIKE '%工程师%'
      OR position LIKE '%产品%'
      OR position LIKE '%设计%'
      OR position LIKE '%测试%'
      OR position LIKE '%数据%'
      OR position LIKE '%安全%'
      OR position LIKE '%运维%' THEN 4
    WHEN position LIKE '%市场%'
      OR position LIKE '%销售%'
      OR position LIKE '%渠道%'
      OR position LIKE '%客户%'
      OR position LIKE '%运营%' THEN 5
    ELSE 2
END
WHERE deleted = 0;

-- 3) Keep existing approvals consistent with the applicant department.
UPDATE oa_approval approval
JOIN sys_user applicant ON applicant.id = approval.applicant_id AND applicant.deleted = 0
SET approval.department_id = applicant.department_id
WHERE approval.deleted = 0;

COMMIT;

-- Check result.
SELECT id, name, leader_id, approver_id, sort_order, status
FROM sys_department
WHERE deleted = 0
ORDER BY sort_order, id;

SELECT u.id, u.username, u.real_name, u.position, u.department_id, d.name AS department_name
FROM sys_user u
LEFT JOIN sys_department d ON d.id = u.department_id AND d.deleted = 0
WHERE u.deleted = 0
ORDER BY u.id;
