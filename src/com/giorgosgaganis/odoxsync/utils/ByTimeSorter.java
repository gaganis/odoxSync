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
package com.giorgosgaganis.odoxsync.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * Created by gaganis on 20/02/17.
 */
public class ByTimeSorter {
    public static void main(String[] args) throws IOException {
        Files.walk(Paths.get("."))
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(f -> {
                    try {
                        return Files.getLastModifiedTime((Path) f);
                    } catch (IOException e) {
                        return null;
                    }
                }).reversed()).limit(11110).forEach(System.out::println);
    }

}
