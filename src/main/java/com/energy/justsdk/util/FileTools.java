package com.energy.justsdk.util;

import java.io.File;

/**
 * @author Bryan
 * @date 2020-01-13
 */
public class FileTools {

    public static File findFileSk(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            throw new RuntimeException(String.format("Matches returned null does %s directory exist?",
                directory.getAbsoluteFile().getName()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(String.format("Expected in %s only 1 sk file but found %s",
                directory.getAbsoluteFile().getName(), matches.length));
        }
        return matches[0];
    }
}
