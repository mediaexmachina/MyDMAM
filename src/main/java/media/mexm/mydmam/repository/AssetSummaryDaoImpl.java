/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2026
 *
 */
package media.mexm.mydmam.repository;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.AssetSummaryEntity;
import media.mexm.mydmam.entity.FileEntity;

@Repository
@Slf4j
public class AssetSummaryDaoImpl implements AssetSummaryDao {

	@Autowired
	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	AssetSummaryRepository assetSummaryRepository;

	/**
	 * @return false if no AssetSummary to file
	 */
	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public boolean getForFile(final FileEntity file) {
		final var assetSummaryList = entityManager.createQuery("""
				SELECT as FROM AssetSummaryEntity as
				WHERE as.file.id = :fileid
				""", AssetSummaryEntity.class)
				.setParameter("fileid", file.getId())
				.getResultList();

		if (assetSummaryList.isEmpty()) {
			return false;
		}

		if (assetSummaryList.size() > 1) {
			final var sorted = assetSummaryList.stream()
					.sorted((l, r) -> l.getCreateDate().compareTo(r.getCreateDate()))
					.toList();

			assetSummaryRepository.deleteAll(sorted.stream()
					.limit(sorted.size() - 1l)
					.toList());

			file.setAssetSummary(sorted.getLast());
		} else {
			file.setAssetSummary(assetSummaryList.get(0));
		}
		return true;
	}

	@Override
	@Transactional(TxType.REQUIRES_NEW)
	public void updateMimeType(final FileEntity file, final String mimeType) {
		if (getForFile(file)) {
			file.getAssetSummary().setMimeType(mimeType);
		} else {
			final var assetSummary = new AssetSummaryEntity(file);
			assetSummary.setMimeType(mimeType);
			file.setAssetSummary(assetSummaryRepository.save(assetSummary));
		}
	}

	@Override
	@Transactional
	public Map<String, AssetSummaryEntity> getAssetSummariesByFileId(final Collection<Integer> ids,
																	 final String realm) {
		final var items = entityManager.createQuery("""
				SELECT new map(DISTINCT(f.hashPath) AS hashPath, asm AS assetSummary)
				FROM FileEntity f
				LEFT JOIN AssetSummaryEntity asm ON asm.file = f
				WHERE
				    f.id IN :ids
				    AND f.realm = :realm
				    AND asm IS NOT NULL
				    AND asm.mimeType != 'application/octet-stream'
				""", Map.class)
				.setParameter("ids", ids)
				.setParameter("realm", realm)
				.getResultList();

		return items.stream()
				.collect(toUnmodifiableMap(
						f -> (String) f.get("hashPath"),
						f -> (AssetSummaryEntity) f.get("assetSummary")));
	}

}
