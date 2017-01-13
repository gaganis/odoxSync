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

/**
 * Created by gaganis on 13/01/17.
 */
public class Region {
    private long offset;
    private long size;

    private long quickDigest;
    private byte[] slowDigest;

    public Region(long offset, long size) {
        this.offset = offset;
        this.size = size;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

    public long getQuickDigest() {
        return quickDigest;
    }

    public void setQuickDigest(long quickDigest) {
        this.quickDigest = quickDigest;
    }

    public byte[] getSlowDigest() {
        return slowDigest;
    }

    public void setSlowDigest(byte[] slowDigest) {
        this.slowDigest = slowDigest;
    }
}
