/*
 * Copyright (C) 2017 Giorgos Gaganis
 *
 * This file is part of odoxSync.
 *
 * odoxSync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * odoxSync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with odoxSync.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.giorgosgaganis.filesynchronizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Created by gaganis on 13/01/17.
 */
public class DirectoryScanner extends Thread {
    private static final Logger logger = Logger.getLogger(DirectoryScanner.class.getName());

    private final ConcurrentHashMap<Integer, File> files;
    AtomicInteger fileIdCounter = new AtomicInteger(1);

    public DirectoryScanner(ConcurrentHashMap<Integer, File> files) {
        this.files = files;
    }

    @Override
    public void run() {

        for (; ; ) {
            if (interrupted())
                return;

            try {
                Path root = Paths.get(".").toAbsolutePath().normalize();
                Files.walk(root)
                        .filter(Files::isRegularFile)
                        .map(Path::normalize)
                        .map(path -> root.relativize(path))
                        .forEach((path) -> {
                            File file = new File(path.toString());
                            if (!files.containsValue(file)) {
                                int id = fileIdCounter.getAndIncrement();
                                file.setId(id);
                                files.put(id, file);

                                logger.fine("Added new tracked file [" + file.getName() + "]");
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                return;
            }
        }

    }

    public static void main(String[] args) {

        Path root = Paths.get("");
        DirectoryScanner ds = new DirectoryScanner(new ConcurrentHashMap<>());
        ds.start();
    }
}
