package de.suitepad.datastore.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * Stub implementation of data sync adapter, which would actually check for server updates and
 * downloads or uploads the latest data. Currently a dummy implementation.
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */
class DataSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String LOGT = DataSyncAdapter.class.getCanonicalName();

    public DataSyncAdapter(Context context, boolean autoInitialize) {

        super(context, autoInitialize);
        Log.v(LOGT, "is now running");
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public DataSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {

        super(context, autoInitialize, allowParallelSyncs);
        Log.e(LOGT, "initializing");
    }


    /**
     * Called by the Android system in response to a request to run the sync adapter. It guarantees
     * that this will be called on a non-UI thread, so it is safe to perform a blocking I/O here.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        Log.v(LOGT, "sync in progress");

        if(checkForDataUpdate()) {

        }
    }

    /**
     * Stub function that would check if there are any updates available at the server side.
     * This approach, isn't recommend as it is akin to server polling (sort of). A better
     * approach is to use gcm to let know the devices that context data on the server has been
     * updated and sync can be started.
     *
     * @return true if data at server has been updated
     */
    private boolean checkForDataUpdate() {

        return true;
    }
}
