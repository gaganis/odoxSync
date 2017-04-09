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
package com.giorgosgaganis.odoxsync.server.net;

import static com.giorgosgaganis.odoxsync.utils.LoggingUtils.configureLogging;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.giorgosgaganis.odoxsync.server.DirectorySynchronizer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * com.giorgosgaganis.filesynchronizer.server.net.MainServer class.
 */
public class MainServer {

    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://0.0.0.0:8081/myapp/";

    private static final Logger logger = Logger.getLogger(DirectorySynchronizer.class.getName());

    public static HttpServer startServer(String workingDirectory) throws IOException {
        configureLogging();
        DirectorySynchronizer.INSTANCE.start(workingDirectory);
        // create a resource config that scans for JAX-RS resources and providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig().packages("com.giorgosgaganis.filesynchronizer.server.net.resources");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        String workingDirectory = args.length > 0 ? args[0] : ".";
        Path absolutePath = Paths.get(workingDirectory).toAbsolutePath();
        if (!Files.exists(absolutePath)) {
            logger.severe("Directory [" + workingDirectory + "] does not exist");
            System.exit(1);
        }
        logger.info("Starting odoxSync server, at directory [" + absolutePath + "]");
        final HttpServer server = startServer(absolutePath.toString());
    }
}

