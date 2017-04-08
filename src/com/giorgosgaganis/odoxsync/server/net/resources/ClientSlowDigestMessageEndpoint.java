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

import com.giorgosgaganis.odoxsync.messages.ClientSlowDigestMessage;
import com.giorgosgaganis.odoxsync.server.DirectorySynchronizer;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("clientslowdigestmessages")
public class ClientSlowDigestMessageEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getIt(List<ClientSlowDigestMessage> clientSlowDigestMessage) {
        try {
            DirectorySynchronizer.INSTANCE.addClientSlowDigests(clientSlowDigestMessage);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}