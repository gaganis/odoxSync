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
package com.giorgosgaganis.odoxsync.messages;

/**
 * Created by gaganis on 01/02/17.
 */
public class ClientSlowDigestMessage {
    private int clientId;
    private int fileId;

    private long offset;
    private byte[] slowDigest;

    public ClientSlowDigestMessage() {
    }

    public ClientSlowDigestMessage(int clientId, int fileId, long offset, byte[] slowDigest) {
        this.clientId = clientId;
        this.fileId = fileId;
        this.offset = offset;
        this.slowDigest = slowDigest;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public byte[] getSlowDigest() {
        return slowDigest;
    }

    public void setSlowDigest(byte[] slowDigest) {
        this.slowDigest = slowDigest;
    }
}

