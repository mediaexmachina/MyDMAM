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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TempStore;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = { "tv.hd3g.jobkit.mod" })
@EnableConfigurationProperties(MyDMAMConfigurationProperties.class)
public class MyDMAMSetup {

	@Bean
	SQLiteConfig getSqliteConfig() {
		final var sqliteConfig = new SQLiteConfig();
		sqliteConfig.enableFullSync(false);
		sqliteConfig.enableLoadExtension(false);
		sqliteConfig.setJournalMode(JournalMode.OFF);
		sqliteConfig.setSynchronous(SynchronousMode.NORMAL);
		sqliteConfig.setTempStore(TempStore.MEMORY);
		return sqliteConfig;
	}

}
