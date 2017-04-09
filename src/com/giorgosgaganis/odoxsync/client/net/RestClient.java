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
package com.giorgosgaganis.odoxsync.client.net;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.client.ClientRegionMessage;
import com.giorgosgaganis.odoxsync.client.RegionDataParams;
import com.giorgosgaganis.odoxsync.messages.BlankFileMessage;
import com.giorgosgaganis.odoxsync.messages.ClientFastDigestMessage;
import com.giorgosgaganis.odoxsync.messages.ClientSlowDigestMessage;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.client.*;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 14/01/17.
 */
public class RestClient {
    private static final Logger logger = Logger.getLogger(RestClient.class.getName());
    private final String SERVER_PATH;

    private Client restClient = ClientBuilder.newClient();
    private int clientId;

    public RestClient(String hostPort) {
        this.SERVER_PATH = "http://" + hostPort + "/odoxsync/";
    }

    public Collection<File> getFiles() {
        WebTarget webTarget = restClient.target(SERVER_PATH + "files");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        Collection<File> files = invocationBuilder.get(new GenericType<Collection<File>>() {
        });

        logger.fine("Retrieved files from server");
        if (logger.isLoggable(Level.FINER)) {
            logger.finest("Files collection content " + files);
        }
        return files;
    }

    public int getClientId() {
        WebTarget webTarget = restClient.target(SERVER_PATH + "introduction");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        Integer id = invocationBuilder.get(Integer.class);
        logger.info("Retrieved clientId [" + id + "]");
        return id;
    }

    public void postClientRegionMessage(ClientRegionMessage clientRegionMessage) {
        logger.fine("Posting ClientRegionMessage");
        WebTarget webTarget = restClient.target(SERVER_PATH + "clientregionmessage");
        Invocation.Builder invocationBuilder =
                webTarget.request();

        Response post = invocationBuilder.post(Entity.entity(clientRegionMessage, MediaType.APPLICATION_JSON_TYPE));
        if(!Response.Status.Family.SUCCESSFUL.equals(post.getStatusInfo().getFamily())){
            logger.severe(post.getStatusInfo().toString());
        }
        post.close();

    }

    public RegionDataParams getRegionData() throws IOException {
        WebTarget webTarget = restClient.target(SERVER_PATH + "regiondata");
        Invocation.Builder invocationBuilder =
                webTarget.request();
        invocationBuilder.header("clientId", clientId);

        Response response = invocationBuilder.get();


        String noTransfer = response.getHeaderString("nothingToTransfer");
        if ("nothingToTransfer".equals(noTransfer)) {
            return null;
        }
        int fileId = Integer.valueOf(response.getHeaderString("fileId"));
        long offset = Long.valueOf(response.getHeaderString("offset"));
        long size = Long.valueOf(response.getHeaderString("size"));

        InputStream inputStream = response.readEntity(InputStream.class);
        byte[] bytes = IOUtils.toByteArray(inputStream);

        return new RegionDataParams(fileId, offset, size, bytes, response);

    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void postFastDigestMessageBatch(ArrayList<ClientFastDigestMessage> batch) {
        logger.fine("Posting batch of ClientFastDigestMessages");
        WebTarget webTarget = restClient.target(SERVER_PATH + "clientfastdigestmessages");
        Invocation.Builder invocationBuilder =
                webTarget.request();

        Response post = invocationBuilder.post(Entity.entity(batch, MediaType.APPLICATION_JSON_TYPE));
        if(!Response.Status.Family.SUCCESSFUL.equals(post.getStatusInfo().getFamily())){
            logger.severe(post.getStatusInfo().toString());
        }
        post.close();
    }

    public void postBlankFileMessage(BlankFileMessage blankFileMessage) {
        logger.fine("Posting BlankFileMessage");
        WebTarget webTarget = restClient.target(SERVER_PATH + "blankfilemessage");
        Invocation.Builder invocationBuilder =
                webTarget.request();

        Response post = invocationBuilder.post(Entity.entity(blankFileMessage, MediaType.APPLICATION_JSON_TYPE));
        if(!Response.Status.Family.SUCCESSFUL.equals(post.getStatusInfo().getFamily())){
            logger.severe(post.getStatusInfo().toString());
        }
        post.close();
    }

    public void postSlowDigestMessageBatch(List<ClientSlowDigestMessage> batch) {
        logger.fine("Posting batch of ClientSlowDigestMessages");
        WebTarget webTarget = restClient.target(SERVER_PATH + "clientslowdigestmessages");
        Invocation.Builder invocationBuilder =
                webTarget.request();

        Entity<List<ClientSlowDigestMessage>> entity = Entity.entity(batch, MediaType.APPLICATION_JSON_TYPE);
        Response post = invocationBuilder.post(entity);
        if(!Response.Status.Family.SUCCESSFUL.equals(post.getStatusInfo().getFamily())){
            logger.severe(post.getStatusInfo().toString());
        }
        post.close();
    }
}
