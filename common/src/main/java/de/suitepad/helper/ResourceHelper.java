package de.suitepad.helper;

import java.io.Closeable;
import java.io.IOException;

/**
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */

public class ResourceHelper {

    public static void close(Closeable closable) {

        try {

            if(closable != null) {

                closable.close();
            }
        } catch (IOException ioe) {

            /* ssh.... */
        }
    }
}
