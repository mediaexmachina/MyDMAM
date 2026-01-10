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
package media.mexm.mydmam.tools;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.onSpinWait;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * Threadsafe
 */
@Slf4j
public class DelayedSync<T> {

	private final int maxEntries;
	private final Duration maxDelay;
	private final Consumer<List<T>> onSync;
	private final ScheduledThreadPoolExecutor scheduledExecutor;
	private final LinkedBlockingQueue<T> queue;
	private final AtomicReference<ScheduledFuture<?>> scheduledTaskReference;

	public DelayedSync(final DelayedSyncConfiguration configuration, final Consumer<List<T>> onSync) {
		requireNonNull(configuration, "\"configuration\" can't to be null");
		maxEntries = configuration.maxEntries();
		maxDelay = configuration.maxDelay();
		this.onSync = Objects.requireNonNull(onSync, "\"onSync\" can't to be null");

		scheduledExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
			final var thread = new Thread(runnable);
			thread.setDaemon(true);
			thread.setName("DelayedSync update");
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setUncaughtExceptionHandler((_, e) -> log.error("Can't update sync", e));
			return thread;
		});

		queue = new LinkedBlockingQueue<>();
		scheduledTaskReference = new AtomicReference<>();
	}

	public synchronized void add(final T entry) {
		queue.add(entry);

		if (queue.size() >= maxEntries) {
			sync();
		} else {
			final var task = scheduledTaskReference.get();
			if (task == null || task.isCancelled() || task.isDone()) {
				final var newTask = scheduledExecutor.schedule(this::sync, maxDelay.toMillis(), MILLISECONDS);
				scheduledTaskReference.set(newTask);
			}
		}
	}

	private synchronized void sync() {
		final var list = new ArrayList<T>();
		queue.drainTo(list);
		if (list.isEmpty() == false) {
			onSync.accept(unmodifiableList(list));
		}
	}

	void waitToSync() {
		while (queue.isEmpty() == false) {
			onSpinWait();
		}
		final var scRef = scheduledTaskReference.get();
		if (scRef != null) {
			try {
				scRef.get(1, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				log.warn("Can't wait ends task", e);
				currentThread().interrupt();
			}
		}
	}

}
