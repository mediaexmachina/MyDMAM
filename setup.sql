-- MyDMAM SQL setup file

-- MySQL / MariaDB compatible file
-- Changes here must be correlated/updated with JPA/Hibernates entities.

CREATE TABLE `file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `directory` tinyint(4) NOT NULL CHECK (`directory` in (0,1)),
  `realm` varchar(64) NOT NULL,
  `storage` varchar(64) NOT NULL,
  `path` varchar(4096) NOT NULL,
  `hash_path` varchar(64) NOT NULL,
  `parent_hash_path` varchar(64) NOT NULL,
  `created` datetime(6) NOT NULL,
  `modified` datetime(6) NOT NULL,
  `length` bigint(20) NOT NULL,
  `watch_last` datetime(6) NOT NULL,
  `watch_marked_as_done` tinyint(4) NOT NULL CHECK (`watch_marked_as_done` in (0,1)),
  `watch_last_is_same` tinyint(4) NOT NULL CHECK (`watch_last_is_same` in (0,1)),
  `watch_done_but_changed` tinyint(4) NOT NULL CHECK (`watch_done_but_changed` in (0,1)),
  PRIMARY KEY (`id`),
  KEY `file_hash_path_idx` (`hash_path`),
  KEY `file_parent_hash_path_idx` (`parent_hash_path`),
  KEY `file_realm_idx` (`realm`),
  KEY `file_storage_idx` (`storage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
