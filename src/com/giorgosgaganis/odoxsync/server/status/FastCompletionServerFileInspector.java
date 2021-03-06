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
package com.giorgosgaganis.odoxsync.server.status;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.Region;

import java.nio.file.attribute.FileTime;

/**
 * Created by gaganis on 31/01/17.
 */
public class FastCompletionServerFileInspector implements Inspector {
    private final File serverFile;

    private final FileTime currentAccessTime;

    private int count = 0;
    private int upToDate = 0;

    public FastCompletionServerFileInspector(File serverFile, FileTime currentAccessTime) {
        this.serverFile = serverFile;
        this.currentAccessTime = currentAccessTime;
    }

    @Override
    public void inspectRegion(Region region) {
        FileTime regionFastModifiedTime = region.getFastModifiedTime();
        if (regionFastModifiedTime != null && currentAccessTime.compareTo(regionFastModifiedTime) >= 0) {
            upToDate++;
        }
        count++;
    }

    @Override
    public void finishInspection() {
        int completion = (int) (100d * upToDate / count);
        serverFile.setFastUpToDatePercent(completion);
    }
}
