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
package com.giorgosgaganis.filesynchronizer.net.server.resources;

import com.giorgosgaganis.filesynchronizer.Client;
import com.giorgosgaganis.filesynchronizer.DirectorySynchronizer;
import com.giorgosgaganis.filesynchronizer.File;
import com.giorgosgaganis.filesynchronizer.TransferCandidate;
import com.giorgosgaganis.filesynchronizer.net.client.Statistics;
import org.glassfish.grizzly.http.server.Response;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("regiondata")

public class RegionData {
    private static final Logger logger = Logger.getLogger(RegionData.class.getName());

    @Context
    Response response;

    @Context
    HttpHeaders httpHeaders;

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces({"application/octet-stream"})
    public StreamingOutput getIt() throws InterruptedException {
        DirectorySynchronizer directorySynchronizer = DirectorySynchronizer.INSTANCE;

        try {
            int clientId = Integer.valueOf(httpHeaders.getHeaderString("clientId"));
            Client client = directorySynchronizer.clients.get(clientId);

            //TODO A race exists here in between polling and adding it to the offered queue
            TransferCandidate transferCandidate;

            if (client == null
                    || (transferCandidate = client.transferCandidateQueue.poll(2, TimeUnit.SECONDS)) == null) {
                response.addHeader("nothingToTransfer", "nothingToTransfer");
                return outputStream -> {
                };
            }
            transferCandidate.setOfferedTimeMillis(System.currentTimeMillis());
            client.offeredTransferCandidates.add(transferCandidate);

            response.addHeader("fileId", transferCandidate.getFileId().toString());
            response.addHeader("offset", transferCandidate.getOffset().toString());
            response.addHeader("size", transferCandidate.getSize().toString());


            File file = directorySynchronizer.files.get(transferCandidate.getFileId());

            return outputStream -> {
                java.nio.file.Path filePath = Paths.get(directorySynchronizer.workingDirectory, file.getName());
                try (
                        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r");
                        FileChannel channel = randomAccessFile.getChannel();
                ) {
                    MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, transferCandidate.getOffset(), transferCandidate.getSize());
                    byte[] arrays = new byte[mappedByteBuffer.remaining()];
                    do {
                        mappedByteBuffer.get(arrays);
                        outputStream.write(arrays);
                    } while (mappedByteBuffer.hasRemaining());

                    Statistics.INSTANCE
                            .bytesTransferred.addAndGet(arrays.length);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "data transmision failed", e);
                    e.printStackTrace();
                } catch (Error e) {
                    logger.log(Level.SEVERE, "data transmision failed", e);
                    e.printStackTrace();
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputStream -> {
        };
    }
}
