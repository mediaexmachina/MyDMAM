-- MyDMAM SQL setup file

-- MySQL / MariaDB compatible file
-- Changes here must be correlated/updated with JPA/Hibernates entities
-- in src/main/java/media/mexm/mydmam/entity

CREATE TABLE `file` (
  `id` int NOT NULL AUTO_INCREMENT,
  `directory` tinyint NOT NULL CHECK (`directory` in (0,1)),
  `realm` varchar(64) NOT NULL,
  `storage` varchar(64) NOT NULL,
  `path` varchar(4096) NOT NULL,
  `hash_path` varchar(64) NOT NULL,
  `parent_hash_path` varchar(64) NOT NULL,
  `modified` datetime NOT NULL,
  `length` bigint NOT NULL,
  `watch_first` datetime NOT NULL,
  `watch_last` datetime NOT NULL,
  `watch_marked_as_done` tinyint NOT NULL CHECK (`watch_marked_as_done` in (0,1)),
  `watch_last_is_same` tinyint NOT NULL CHECK (`watch_last_is_same` in (0,1)),
  `watch_done_but_changed` tinyint NOT NULL CHECK (`watch_done_but_changed` in (0,1)),
  PRIMARY KEY (`id`),
  KEY `file_hash_path_idx` (`hash_path`),
  KEY `file_parent_hash_path_idx` (`parent_hash_path`),
  KEY `file_realm_idx` (`realm`),
  KEY `file_storage_idx` (`storage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `pending_activity` (
  `id` int NOT NULL AUTO_INCREMENT,
  `create_date` datetime NOT NULL,
  `file_id` int NOT NULL,
  `handler_name` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `previous_handlers` varchar(2048) NOT NULL,
  `updated` datetime NOT NULL,
  `worker_host` varchar(128) NOT NULL,
  `worker_pid` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `pending_activity_worker_host_idx` (`worker_host`),
  KEY `pending_activity_updated_idx` (`updated`),
  KEY `pending_activity_file_id_idx` (`file_id`),
  CONSTRAINT pending_activity_file_id_fk FOREIGN KEY (file_id) REFERENCES file(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `file_metadata` (
  `id` int NOT NULL AUTO_INCREMENT,
  `create_date` datetime NOT NULL,
  `file_id` int NOT NULL,
  `origin` varchar(32) NOT NULL,
  `classifier` varchar(32) NOT NULL,
  `layer` int NOT NULL,
  `key_name` varchar(128) NOT NULL,
  `key_value` varchar(1024) NOT NULL,
  `entry_crc` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `file_metadata_origin_idx` (`origin`),
  KEY `file_metadata_classifier_idx` (`classifier`),
  KEY `file_metadata_key_name_idx` (`key_name`),
  KEY `file_metadata_entry_crc_idx` (`entry_crc`),
  KEY `file_metadata_file_id_idx` (`file_id`),
  CONSTRAINT file_metadata_file_id_fk FOREIGN KEY (file_id) REFERENCES file(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `asset_renderedfile` (
  `id` int NOT NULL AUTO_INCREMENT,
  `create_date` datetime NOT NULL,
  `file_id` int NOT NULL,
  `name` varchar(256) NOT NULL,
  `indexref` int NOT NULL,
  `length` bigint NOT NULL,
  `preview_type` varchar(128) NOT NULL,
  `mime_type` varchar(128) NOT NULL,
  `encoded` varchar(16) NOT NULL,
  `etag` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `asset_renderedfile_file_id_idx` (`file_id`),
  CONSTRAINT asset_renderedfile_file_id_fk FOREIGN KEY (file_id) REFERENCES file(id) ON DELETE CASCADE,
  CONSTRAINT asset_renderedfile_uc UNIQUE (`file_id`, `name`, `indexref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `asset_textextractedfile` (
  `id` int NOT NULL AUTO_INCREMENT,
  `create_date` datetime NOT NULL,
  `file_id` int NOT NULL,
  `name` varchar(256) NOT NULL,
  `length` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `asset_textextractedfile_file_id_idx` (`file_id`),
  CONSTRAINT asset_textextractedfile_file_id_fk FOREIGN KEY (file_id) REFERENCES file(id) ON DELETE CASCADE,
  CONSTRAINT asset_textextractedfile_uc UNIQUE (`file_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
