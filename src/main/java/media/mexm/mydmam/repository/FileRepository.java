/*
 * This file is part of mydmam.
 *
 * This program is free softare; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Softare Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it ill be useful,
 * but WITHOUT ANY WARRANTY; ithout even the implied arranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import media.mexm.mydmam.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

	@Query("SELECT f FROM FileEntity f WHERE f.hashPath = :hashPath")
	FileEntity getByHashPath(String hashPath);

	@Query("SELECT f FROM FileEntity f WHERE f.hashPath IN :hashPath")
	Set<FileEntity> getByHashPath(Set<String> hashPath);

	@Query("""
			SELECT f FROM FileEntity f
			WHERE f.watchMarkedAsDone = false
			AND ((f.directory = true AND :pickUpDirs = true) OR (f.directory = false AND :pickUpFiles = true))
			AND f.hashPath NOT IN :detectedHashPath
			AND f.realm = :realm
			AND f.storage = :storage
			""")
	Set<FileEntity> getLostedByHashPath(Set<String> detectedHashPath,
										boolean pickUpDirs,
										boolean pickUpFiles,
										String realm,
										String storage);

	@Query("""
			SELECT f FROM FileEntity f
			WHERE f.watchMarkedAsDone = false
			AND ((f.directory = true AND :pickUpDirs = true) OR (f.directory = false AND :pickUpFiles = true))
			AND f.realm = :realm
			AND f.storage = :storage
			""")
	Set<FileEntity> getLostedForEmptyDir(boolean pickUpDirs,
										 boolean pickUpFiles,
										 String realm,
										 String storage);

	@Query("""
			SELECT f.hashPath FROM FileEntity f WHERE f.realm = :realm AND f.storage = :storage
			""")
	Set<String> getAllHashPathByStorage(String realm,
										String storage);

	@Query("""
			SELECT DISTINCT f.realm FROM FileEntity f
			""")
	Set<String> getAllRealms();

	@Query("""
			SELECT DISTINCT f.storage FROM FileEntity f WHERE f.realm = :realm
			""")
	Set<String> getAllStoragesByRealm(String realm);

	@Query("DELETE FROM FileEntity WHERE hashPath IN :detectedHashPath")
	@Modifying
	void deleteByHashPath(Set<String> detectedHashPath);

	@Query("SELECT COUNT(f) FROM FileEntity f WHERE f.realm = :realm AND f.storage = :storage")
	int countByStorage(String realm,
					   String storage);

	@Query("SELECT COUNT(f) FROM FileEntity f WHERE f.realm = :realm")
	int countByRealm(String realm);

	@Query("""
			SELECT COUNT(f) FROM FileEntity f
			WHERE ((f.directory = true AND :pickUpDirs = true) OR (f.directory = false AND :pickUpFiles = true))
			AND f.realm = :realm
			AND f.storage = :storage
			""")
	int countByStorage(boolean pickUpDirs,
					   boolean pickUpFiles,
					   String realm,
					   String storage);

	@Query("DELETE FROM FileEntity WHERE realm = :realm AND storage NOT IN :storageToKeep")
	@Modifying
	void deleteStorageNotInSet(String realm, Set<String> storageToKeep);

}
