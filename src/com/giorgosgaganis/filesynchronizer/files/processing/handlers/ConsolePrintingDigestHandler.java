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
package com.giorgosgaganis.filesynchronizer.files.processing.handlers;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;

import java.nio.file.attribute.FileTime;
import java.util.Base64;

/**
 * Created by gaganis on 23/01/17.
 */
public class ConsolePrintingDigestHandler implements FastDigestHandler, SlowDigestHandler {
    @Override
    public void handleFastDigest(byte[] buffer, File file, Region currentRegion, Integer fastDigest, FileTime fileLastModifiedTime) {
        System.out.print("currentOffset = " + currentRegion.getOffset());
        System.out.println(", fastDigest = " + fastDigest);
    }

    @Override
    public void handleSlowDigest(File file, Region currentRegion, FileTime batchLastModifiedTime, byte[] slowDigest) {

        System.out.print("currentOffset = " + currentRegion.getOffset());
        System.out.println(", slowDigest = " + Base64.getEncoder().encodeToString(slowDigest));
    }
}
