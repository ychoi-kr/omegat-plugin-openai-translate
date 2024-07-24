package org.omegat.util;

import java.io.File;

public class DirectoryMonitor extends Thread {
    /**
     * Callback for monitoring.
     */
    public interface Callback {
        /**
         * Called on any file changes - created, modified, deleted.
         */
        void fileChanged(File file);
    }

    public interface DirectoryCallback {
        /**
         * Called once for every directory where a file was changed - created, modified, deleted.
         */
        void directoryChanged(File file);
    }

}
