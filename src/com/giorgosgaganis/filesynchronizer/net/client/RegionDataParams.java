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
package com.giorgosgaganis.filesynchronizer.net.client;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.InputStream;

/**
 * Created by gaganis on 16/01/17.
 */
public class RegionDataParams {

    final int fileId;
    final long offset;
    final long size;
    final InputStream inputStream;

    public RegionDataParams(int fileId, long offset, long size, InputStream inputStream) {

        this.fileId = fileId;
        this.offset = offset;
        this.size = size;
        this.inputStream = inputStream;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("fileId", fileId)
                .append("offset", offset)
                .append("size", size)
                .append("inputStream", inputStream)
                .toString();
    }
}
