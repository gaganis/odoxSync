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

import static com.giorgosgaganis.filesynchronizer.Contants.REGION_SIZE;

/**
 * Created by gaganis on 14/01/17.
 */
public class RegionCalculator {
    private final File file;
    private final String workingDirectory;

    public RegionCalculator(String workingDirectory, File file) {
        this.workingDirectory = workingDirectory;
        this.file = file;

    }

    public void calculate() throws IOException {
        Path path = Paths.get(workingDirectory, file.getName());
        long fileSize = Files.size(path);

        calculateForSize(fileSize);
    }

    public void calculateForSize(long fileSize) {
        file.setSize(fileSize);

        ConcurrentHashMap<Long, Region> regions = file.getRegions();

        long position = 0;
        do {

            long regionSize =
                    position + REGION_SIZE > fileSize
                            ? fileSize - position
                            : REGION_SIZE;
            regions.putIfAbsent(position, new Region(position, regionSize));

            position += REGION_SIZE;
        } while (position < fileSize);
    }
}
