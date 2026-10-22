USE `mpi_system`;

CREATE TABLE `facility` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `code` varchar(50) UNIQUE NOT NULL,
    `name` varchar(255) NOT NULL,
    `facility_type` ENUM('HOSPITAL', 'CLINIC', 'OTHER') NOT NULL DEFAULT 'OTHER',
    `address` varchar(500),
    `phone_number` varchar(20),
    `is_active` boolean NOT NULL DEFAULT true,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `source_system` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `facility_id` bigint NOT NULL,
    `code` varchar(50) UNIQUE NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(500),
    `is_active` boolean NOT NULL DEFAULT true,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `patient_master` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `enterprise_id` varchar(30) UNIQUE NOT NULL,
    `full_name` varchar(255) NOT NULL,
    `date_of_birth` date,
    `gender` ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER',
    `national_id` varchar(20),
    `health_insurance_no` varchar(20),
    `phone_number` varchar(20),
    `address` varchar(500),
    `status` ENUM(
        'ACTIVE',
        'INACTIVE',
        'MERGED'
    ) NOT NULL DEFAULT 'ACTIVE',
    `merged_into_id` bigint DEFAULT NULL COMMENT 'ID của patient_master đích nếu bản ghi này bị gộp',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `patient` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `source_system_id` bigint NOT NULL,
    `local_patient_code` varchar(100) NOT NULL COMMENT 'mã BN tại hệ thống nguồn',
    `full_name` varchar(255) NOT NULL,
    `date_of_birth` date,
    `gender` ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER',
    `national_id` varchar(20),
    `health_insurance_no` varchar(20),
    `phone_number` varchar(20),
    `address` varchar(500),
    `master_patient_id` bigint COMMENT 'FK -> patient_master sau khi đối chiếu xong',
    `match_status` ENUM(
        'PENDING',
        'MATCHED',
        'NEW_MASTER',
        'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `users` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `email` varchar(255) UNIQUE NOT NULL,
    `password` varchar(255) NOT NULL,
    `address` varchar(500),
    `gender` ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER',
    `avatar` varchar(500),
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `role` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `name` varchar(100) UNIQUE NOT NULL COMMENT 'ADMIN, REVIEWER, VIEWER...',
    `description` varchar(500),
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `user_role` (
    `user_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    `assigned_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `role_id`)
);

CREATE TABLE `match_candidate` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `patient_id` bigint NOT NULL,
    `candidate_master_id` bigint NOT NULL,
    `match_score` decimal(5, 2) NOT NULL,
    `score_breakdown` json COMMENT '{"name":30,"dob":25,"nationalId":0,...}',
    `weight_version` varchar(20) NOT NULL DEFAULT 'v1' COMMENT 'tham chiếu config cứng trong code',
    `decision` ENUM(
        'PENDING',
        'AUTO_APPROVED',
        'MANUAL_APPROVED',
        'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',
    `reviewed_by` bigint,
    `reviewed_at` datetime,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `match_decision_log` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `match_candidate_id` bigint NOT NULL,
    `action` ENUM('MERGE', 'REJECT', 'UNMERGE') NOT NULL,
    `performed_by` bigint,
    `reason` varchar(500),
    `before_state` json,
    `after_state` json,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX `patient_index_0` ON `patient` (
    `source_system_id`,
    `local_patient_code`
);

ALTER TABLE `source_system`
ADD FOREIGN KEY (`facility_id`) REFERENCES `facility` (`id`);

ALTER TABLE `patient_master`
ADD FOREIGN KEY (`merged_into_id`) REFERENCES `patient_master` (`id`);

ALTER TABLE `patient`
ADD FOREIGN KEY (`source_system_id`) REFERENCES `source_system` (`id`);

ALTER TABLE `patient`
ADD FOREIGN KEY (`master_patient_id`) REFERENCES `patient_master` (`id`);

ALTER TABLE `user_role`
ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

ALTER TABLE `user_role`
ADD FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE;

ALTER TABLE `match_candidate`
ADD FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE CASCADE;

ALTER TABLE `match_candidate`
ADD FOREIGN KEY (`candidate_master_id`) REFERENCES `patient_master` (`id`) ON DELETE CASCADE;

ALTER TABLE `match_candidate`
ADD FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

ALTER TABLE `match_decision_log`
ADD FOREIGN KEY (`match_candidate_id`) REFERENCES `match_candidate` (`id`) ON DELETE CASCADE;

ALTER TABLE `match_decision_log`
ADD FOREIGN KEY (`performed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

CREATE TABLE `import_job` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `file_name` varchar(255) NOT NULL,
    `file_size` bigint NOT NULL COMMENT 'Kích thước file (bytes)',
    `file_type` ENUM('CSV', 'XLSX') NOT NULL,
    `source_system_id` bigint NOT NULL,
    `status` ENUM(
        'PENDING',
        'VALIDATING',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',
    `total_rows` int DEFAULT 0,
    `processed_rows` int DEFAULT 0,
    `successful_rows` int DEFAULT 0,
    `failed_rows` int DEFAULT 0,
    `duplicate_rows` int DEFAULT 0,
    `warning_rows` int DEFAULT 0,
    `started_at` datetime,
    `completed_at` datetime,
    `error_message` text,
    `configuration` json COMMENT 'Cấu hình import: mapping, rules, etc.',
    `created_by` bigint NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`source_system_id`) REFERENCES `source_system` (`id`),
    FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
);

CREATE TABLE `import_job_detail` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `import_job_id` bigint NOT NULL,
    `row_number` int NOT NULL,
    `row_data` json NOT NULL COMMENT 'Dữ liệu gốc của row',
    `status` ENUM(
        'SUCCESS',
        'FAILED',
        'DUPLICATE',
        'WARNING'
    ) NOT NULL,
    `match_score` decimal(5, 2) COMMENT 'Điểm match nếu tìm thấy duplicate',
    `patient_id` bigint COMMENT 'ID của patient đã tạo (nếu SUCCESS)',
    `matched_master_id` bigint COMMENT 'ID của patient_master đã match (nếu DUPLICATE)',
    `error_message` text,
    `warning_message` text,
    `processed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`import_job_id`) REFERENCES `import_job` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE SET NULL,
    FOREIGN KEY (`matched_master_id`) REFERENCES `patient_master` (`id`) ON DELETE SET NULL
);

USE `mpi_system`;

CREATE TABLE `algorithm_weight_config` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `version` varchar(20) UNIQUE NOT NULL COMMENT 'Format: v2.4, v2.5...',
    `name_weight` decimal(5, 2) NOT NULL,
    `dob_weight` decimal(5, 2) NOT NULL,
    `national_id_weight` decimal(5, 2) NOT NULL,
    `phone_weight` decimal(5, 2) NOT NULL,
    `address_weight` decimal(5, 2) NOT NULL,
    `auto_approval_threshold` decimal(5, 2) NOT NULL,
    `manual_review_threshold` decimal(5, 2) NOT NULL,
    `is_active` boolean NOT NULL DEFAULT false,
    `description` varchar(500),
    `created_by` varchar(255),
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deployed_at` datetime DEFAULT NULL
);

ALTER TABLE import_job
ADD COLUMN file_token VARCHAR(100) NULL AFTER id;

UPDATE import_job
SET
    file_token = file_name
WHERE
    file_token IS NULL;

CREATE TABLE `refresh_token` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `token` varchar(500) UNIQUE NOT NULL,
    `expires_at` datetime NOT NULL,
    `is_revoked` boolean NOT NULL DEFAULT false,
    `device_info` varchar(255),
    `ip_address` varchar(45),
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `used_at` datetime,
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

USE `mpi_system`;

ALTER TABLE `users`
ADD COLUMN `status` ENUM(
    'ACTIVE',
    'INACTIVE',
    'SUSPENDED'
) NOT NULL DEFAULT 'ACTIVE' AFTER `avatar`;