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
package com.giorgosgaganis.odoxsync.server.net.resources;

import com.giorgosgaganis.odoxsync.Client;
import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.server.DirectorySynchronizer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Path("progress")
public class Progress {

    private static class FileProgress {
        String name;
        int metadataSynced;
        int fastDigestScanned;
        int slowDigestScanned;

        public String getName() {
            return name;
        }

        public int getMetadataSynced() {
            return metadataSynced;
        }

        public int getFastDigestScanned() {
            return fastDigestScanned;
        }

        public int getSlowDigestScanned() {
            return slowDigestScanned;
        }
    }

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, List<FileProgress>> getIt() {
        ConcurrentHashMap<Integer, Client> clients = DirectorySynchronizer.INSTANCE.clients;
        ConcurrentHashMap<Integer, File> serverFiles = DirectorySynchronizer.INSTANCE.files;

        Map<Integer, List<FileProgress>> result = new HashMap<>();

        for (Client client : clients.values()) {

            List<FileProgress> fileProgresses = getFileProgresses(serverFiles, client);
            result.put(client.getId(), fileProgresses);
        }
        return result;
    }

    private List<FileProgress> getFileProgresses(ConcurrentHashMap<Integer, File> serverFiles, Client client) {
        List<FileProgress> fileProgresses = new ArrayList<>();
        ConcurrentHashMap<Integer, File> files = client.getFiles();
        for (File file : files.values()
                .stream()
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList())) {
            FileProgress fileProgress = new FileProgress();
            fileProgress.name = file.getName();
            fileProgress.metadataSynced = file.getMetadataReceivedPercent();

            File serverFile = serverFiles.get(file.getId());
            fileProgress.fastDigestScanned = serverFile.getFastUpToDatePercent();
            fileProgress.slowDigestScanned = serverFile.getSlowUpToDatePercent();
            fileProgresses.add(fileProgress);
        }
        return fileProgresses;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileProgress> getForClient(@PathParam("id") int id) {
        ConcurrentHashMap<Integer, Client> clients = DirectorySynchronizer.INSTANCE.clients;
        ConcurrentHashMap<Integer, File> serverFiles = DirectorySynchronizer.INSTANCE.files;

        return getFileProgresses(serverFiles, clients.get(id));
    }
}
