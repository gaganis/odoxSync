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
package com.giorgosgaganis.odoxsync.client;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.ws.rs.core.Response;

/**
 * Created by gaganis on 16/01/17.
 */
public class RegionDataParams {

    final int fileId;
    final long offset;
    final long size;
    final Response response;
    final byte[] bytes;

    public RegionDataParams(int fileId, long offset, long size, byte[] bytes, Response response) {

        this.fileId = fileId;
        this.offset = offset;
        this.size = size;
        this.bytes = bytes;
        this.response = response;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("fileId", fileId)
                .append("offset", offset)
                .append("size", size)
                .toString();
    }
}
