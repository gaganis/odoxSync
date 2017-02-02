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
package com.giorgosgaganis.filesynchronizer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;

/**
 * Created by gaganis on 14/01/17.
 */
public class LoggingUtils {
    public static void configureLogging() throws IOException {
        Path logConfig = Paths.get("logging.properties");
        if (Files.exists(logConfig)) {
            LogManager.getLogManager().readConfiguration(Files.newInputStream(logConfig));
        } else {
          logConfig = Paths.get("/home/gaganis/IdeaProjects/DirectorySynchronizer", "logging.properties");
            if (Files.exists(logConfig)) {
                LogManager.getLogManager().readConfiguration(Files.newInputStream(logConfig));
            }
        }

    }
}
