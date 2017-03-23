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
package com.giorgosgaganis.odoxsync.server;

import com.giorgosgaganis.odoxsync.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

/**
 * Created by gaganis on 20/02/17.
 */
public class FileModifiedComparator implements Comparator<File> {

    public int compare(File file1, File file2) {
        FileTime time1 = getFileTime(file1);
        FileTime time2 = getFileTime(file2);
        if(time1 == null && time2 == null) {
            return 0;
        }
        if(time1 == null) {
            return -1;
        }

        if(time2 == null) {
            return 1;
        }

        return time1.compareTo(time2);
    }

    private FileTime getFileTime(File file) {
        try{
            return Files.getLastModifiedTime(file.getAbsolutePath());
        } catch (IOException e) {
            return null;
        }
    }
}
