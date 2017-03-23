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
import com.giorgosgaganis.odoxsync.server.candidates.TransferCandidate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Path("status")
public class Status {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getIt() {
        ConcurrentHashMap<Integer, Client> clients = DirectorySynchronizer.INSTANCE.clients;

        return clients.values()
                .stream()
                .flatMap(client -> client.offeredTransferCandidates.stream().map(TransferCandidate::getFileId))
                .distinct()
                .map(DirectorySynchronizer.INSTANCE.files::get)
                .map(File::getName)
                .collect(Collectors.toList());
    }
}
