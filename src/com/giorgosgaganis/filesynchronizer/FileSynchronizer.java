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
import java.util.logging.LogManager;

/**
 * Created by gaganis on 13/01/17.
 */
public class FileSynchronizer {

    private final ConcurrentHashMap<Integer, File> files = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        configureLogging();


    }

    private static void configureLogging() throws IOException {
        Path logConfig = Paths.get("logging.properties");
        if (Files.exists(logConfig)) {
            LogManager.getLogManager().readConfiguration(Files.newInputStream(logConfig));
        }
    }
}
