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
package com.giorgosgaganis.odoxsync.server.files;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.Region;
import com.giorgosgaganis.odoxsync.files.processing.handlers.FastDigestHandler;

import java.nio.file.attribute.FileTime;

public class FileRegionHashMapDigestHandler implements FastDigestHandler {

    @Override
    public void handleFastDigest(byte[] buffer, File file, Region currentRegion, Integer fastDigest, FileTime fileLastModifiedTime) {
        Region region = file.getRegions().get(currentRegion.getOffset());
        region.setQuickDigest(fastDigest);
        region.setFastModifiedTime(fileLastModifiedTime);
    }
}