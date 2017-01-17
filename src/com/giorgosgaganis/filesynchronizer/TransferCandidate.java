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

import com.google.common.base.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by gaganis on 15/01/17.
 */
public class TransferCandidate {
    private final Integer fileId;
    private final Long offset;
    private final Long size;
    private long offeredTimeMillis;

    public TransferCandidate(Integer fileId, Long offset, Long size) {

        this.fileId = fileId;
        this.offset = offset;
        this.size = size;
    }

    public Integer getFileId() {
        return fileId;
    }

    public Long getOffset() {
        return offset;
    }

    public Long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferCandidate that = (TransferCandidate) o;
        return Objects.equal(fileId, that.fileId) &&
                Objects.equal(offset, that.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileId, offset);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("fileId", fileId)
                .append("offset", offset)
                .append("size", size)
                .toString();
    }

    public void setOfferedTimeMillis(long offeredTimeMillis) {
        this.offeredTimeMillis = offeredTimeMillis;
    }

    public long getOfferedTimeMillis() {
        return offeredTimeMillis;
    }
}
