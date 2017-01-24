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
package com.giorgosgaganis.filesynchronizer.server.net.resources;

import com.giorgosgaganis.filesynchronizer.Client;
import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.server.DirectorySynchronizer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("progress")
public class Progress {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, Map<String, Integer>> getIt() {
        ConcurrentHashMap<Integer, Client> clients = DirectorySynchronizer.INSTANCE.clients;

        Map<Integer, Map<String, Integer>> result = new HashMap<>();

        for (Client client : clients.values()) {
            HashMap<String, Integer> resultFileMap = new HashMap<>();
            result.put(client.getId(), resultFileMap);

            ConcurrentHashMap<Integer, File> files = client.getFiles();
            for (File file : files.values()) {
                resultFileMap.put(file.getName(), file.getSyncedPercentage());
            }

        }
        return result;
    }
}
