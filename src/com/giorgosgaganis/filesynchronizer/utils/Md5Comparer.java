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
package com.giorgosgaganis.filesynchronizer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gaganis on 16/02/17.
 */
public class Md5Comparer {
    public static void main(String[] args) throws IOException {
        Path file0 = Paths.get(args[0]);
        Path file1 = Paths.get(args[1]);

        Map<String, String> map0 = new HashMap<>();
        Map<String, String> map1 = new HashMap<>();

        Files.lines(file0).forEach((line) -> map0.put(line.substring(33), line.substring(0, 32)));
        Files.lines(file1).forEach((line) -> map1.put(line.substring(33), line.substring(0, 32)));

        Set<String> allKeys = new HashSet<>(map0.keySet());
        allKeys.addAll(map1.keySet());

        for (String key : allKeys) {
//            System.out.println("key = " + key);
            String value0 = map0.get(key);
            String value1 = map1.get(key);

            if (value0 == null && value1 == null) {
                continue;
            }
            if (value0 != null && !value0.equals(value1)) {
                System.out.print("key = " + key);
                System.out.print(", value0 = " + value0);
                System.out.println(", value1 = " + value1);
                continue;
            }

            if (value1 != null && !value1.equals(value0)) {
                System.out.print("key = " + key);
                System.out.print(", value0 = " + value0);
                System.out.println(", value1 = " + value1);
            }
        }

    }
}
