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

import com.giorgosgaganis.filesynchronizer.Client;
import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by gaganis on 29/01/17.
 */
public class RegionWalker {
    private static final Logger logger = Logger.getLogger(RegionWalker.class.getName());


    private final ConcurrentHashMap<Integer, File> files;
    private final ConcurrentHashMap<Integer, Client> clients;


    public RegionWalker(ConcurrentHashMap<Integer, File> files,
                        ConcurrentHashMap<Integer, Client> clients) {
        this.files = files;
        this.clients = clients;
    }

    public void walk() throws IOException {
        walkServerFiles();
    }

    private void walkServerFiles() throws IOException {

        for (File serverFile : files.values()) {
            walkClientsForServerFile(serverFile);
        }
    }

    private void walkClientsForServerFile(File serverFile) throws IOException {

        FileTime modifiedTime = Files.getLastModifiedTime(serverFile.getAbsolutePath());

        List<Inspector> inspectors = new ArrayList<>(getServerFileInspectors(serverFile, modifiedTime));

        for (Client client : clients.values()) {
            File clientFile = client.files.get(serverFile.getId());
            if(clientFile != null) {
                inspectors.addAll(getClientFileInspectors(clientFile));
            }
        }
        inspect(serverFile, inspectors);
    }

    private void inspect(File serverFile, List<Inspector> inspectors) {

        for (Region serverRegion : serverFile.getRegions().values()) {
            for (Inspector inspector : inspectors) {
                inspector.inspectRegion(serverRegion);
            }
        }

        for (Inspector inspector : inspectors) {
            inspector.finishInspection();
        }
    }

    private List<Inspector> getClientFileInspectors(File clientFile) {
        List<Inspector> inspectors = new ArrayList<>();
        inspectors.add(new ClientMetadataReceivedInspector(clientFile));
        return inspectors;
    }

    private List<Inspector> getServerFileInspectors(File serverFile, FileTime modifiedTime) {
        List<Inspector> inspectors = new ArrayList<>();
        inspectors.add(
                new FastCompletionServerFileInspector(serverFile, modifiedTime));
        inspectors.add(
                new SlowCompletionServerFileInspector(serverFile, modifiedTime));
        return inspectors;
    }
}
