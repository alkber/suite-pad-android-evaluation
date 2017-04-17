package de.suitepad.helper;

/**
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */

public class FileHelper {

    public static String mime(String fileName) {

        if (fileName == null || fileName.length() == 0) {

            return null;
        } else if (fileName.endsWith(".html")) {

            return "text/html";
        } else if (fileName.endsWith(".js")) {

            return "application/javascript";
        } else if (fileName.endsWith(".css")) {

            return "text/css";
        } else if (fileName.endsWith("json")) {

            return "application/json";
        } else {

            return "application/octet-stream";
        }
    }
}

