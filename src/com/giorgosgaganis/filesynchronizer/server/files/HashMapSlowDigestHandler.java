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
package com.giorgosgaganis.filesynchronizer.server.files;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.files.processing.handlers.SlowDigestHandler;

import java.nio.file.attribute.FileTime;

/**
 * Created by gaganis on 02/02/17.
 */
public class HashMapSlowDigestHandler implements SlowDigestHandler {
    public void handleSlowDigest(File file, Region currentRegion, FileTime batchLastModifiedTime, byte[] slowDigest) {
        Region region = file.getRegions().get(currentRegion.getOffset());
        region.setSlowDigest(slowDigest);
        region.setSlowModifiedTime(batchLastModifiedTime);
    }
}
