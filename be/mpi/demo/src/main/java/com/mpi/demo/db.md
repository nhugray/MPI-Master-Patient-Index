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