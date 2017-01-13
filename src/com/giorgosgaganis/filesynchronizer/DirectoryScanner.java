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

/**
 * Created by gaganis on 13/01/17.
 */
public class DirectoryScanner extends Thread {
    private final ConcurrentHashMap<Integer, File> files;

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
                        .map(path -> Paths.get("/home/gaganis/temp/").resolve(path))
                        .forEach((path) -> {
                            Path parent = path.getParent();
                            try {
                                if(Files.notExists(path)) {
                                    Files.createDirectories(parent);
                                    Files.createFile(path);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
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
        DirectoryScanner ds = new DirectoryScanner(null);
        ds.start();
    }
}
