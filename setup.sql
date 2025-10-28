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
