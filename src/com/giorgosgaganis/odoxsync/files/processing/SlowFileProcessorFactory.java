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
package com.giorgosgaganis.odoxsync.files.processing;

import com.giorgosgaganis.odoxsync.File;
import com.giorgosgaganis.odoxsync.files.processing.handlers.SlowDigestHandler;

/**
 * Created by gaganis on 24/01/17.
 */
public class SlowFileProcessorFactory implements FileProcessorFactory {
    private final SlowDigestHandler slowDigestHandler;

    public SlowFileProcessorFactory(SlowDigestHandler slowDigestHandler) {
        this.slowDigestHandler = slowDigestHandler;
    }

    @Override
    public FileProcessor create(File file) {
        return new SlowFileProcessor(slowDigestHandler, file);
    }
}
