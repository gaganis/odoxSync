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
package com.giorgosgaganis.filesynchronizer.server.status;

import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaganis on 01/02/17.
 */
public class ClientMetadataReceivedInspector implements Inspector {
    private final File clientFile;

    private int count = 0;
    private int receivedCount = 0;

    public ClientMetadataReceivedInspector(File clientFile) {
        this.clientFile = clientFile;
    }

    @Override
    public void inspectRegion(Region region) {
        count++;

        ConcurrentHashMap<Long, Region> clientFileRegions = clientFile.getRegions();
        if(clientFileRegions.containsKey(region.getOffset())){
           receivedCount++;
        }
    }

    @Override
    public void finishInspection() {
        int receivedPercent = (int) (100d * receivedCount / count);
        clientFile.setMetadataReceivedPercent(receivedPercent);
    }
}
