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
package com.giorgosgaganis.filesynchronizer.net.client;

import com.giorgosgaganis.filesynchronizer.File;

import javax.ws.rs.client.*;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaganis on 14/01/17.
 */
public class RestClient {
    private static final Logger logger = Logger.getLogger(RestClient.class.getName());

    private Client restClient = ClientBuilder.newClient();
    private int clientId;

    public Collection<File> getFiles() {
        WebTarget webTarget = restClient.target("http://localhost:8081/myapp/files");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        Collection<File> files = invocationBuilder.get(new GenericType<Collection<File>>() {
        });

        logger.info("Retrieved files from server");
        if (logger.isLoggable(Level.FINER)) {
            logger.finest("Files collection content " + files);
        }
        return files;
    }

    public int getClientId() {
        WebTarget webTarget = restClient.target("http://localhost:8081/myapp/introduction");

        Invocation.Builder invocationBuilder =
                webTarget.request();

        Integer id = invocationBuilder.get(Integer.class);
        logger.info("Retrieved clientId [" + id + "]");
        return id;
    }

    public void postClientRegionMessage(ClientRegionMessage clientRegionMessage) {
        logger.fine("Posting ClientRegionMessage");
        WebTarget webTarget = restClient.target("http://localhost:8081/myapp/clientregionmessage");
        Invocation.Builder invocationBuilder =
                webTarget.request();

        invocationBuilder.post(Entity.entity(clientRegionMessage, MediaType.APPLICATION_JSON_TYPE));
    }

    public RegionDataParams getRegionData() throws IOException {
        WebTarget webTarget = restClient.target("http://localhost:8081/myapp/regiondata");
        Invocation.Builder invocationBuilder =
                webTarget.request();
        invocationBuilder.header("clientId", clientId);

        Response response = invocationBuilder.get();


        String noTransfer = response.getHeaderString("nothingToTransfer");
        if("nothingToTransfer".equals(noTransfer)){
            return null;
        }
        int fileId = Integer.valueOf(response.getHeaderString("fileId"));
        long offset = Long.valueOf(response.getHeaderString("offset"));
        long size = Long.valueOf(response.getHeaderString("size"));

        InputStream inputStream = response.readEntity(InputStream.class);

        return new RegionDataParams(fileId, offset, size, inputStream);

    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
}
