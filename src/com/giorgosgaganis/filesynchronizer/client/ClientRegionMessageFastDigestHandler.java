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
package com.giorgosgaganis.filesynchronizer.client;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;
import com.giorgosgaganis.filesynchronizer.files.FastDigestHandler;

import java.nio.file.attribute.FileTime;

/**
 * Created by gaganis on 24/01/17.
 */
public class ClientRegionMessageFastDigestHandler implements FastDigestHandler {

    private final int clientId;
    private final ClientRegionMessageHandler clientRegionMessageHandler;

    public ClientRegionMessageFastDigestHandler(int clientId, ClientRegionMessageHandler clientRegionMessageHandler) {
        this.clientId = clientId;
        this.clientRegionMessageHandler = clientRegionMessageHandler;
    }

    @Override
    public void handleFastDigest(byte[] buffer, File file, Region currentRegion, Integer fastDigest, FileTime fileLastModifiedTime) {
        clientRegionMessageHandler.submitClientRegionMessage(clientId, file, currentRegion.getOffset(), currentRegion.getSize(), fastDigest, null);
    }
}
