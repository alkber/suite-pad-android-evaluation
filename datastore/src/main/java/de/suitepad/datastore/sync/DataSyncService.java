package de.suitepad.datastore.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/** Service to handle sync requests.
 *
 * <p>This service is invoked in response to Intents with action android.content.DataSyncAdapter, and
 * returns a Binder connection to DataSyncAdapter.
 *
 * <p>For performance, only one sync adapter will be initialized within this application's context.
 *
 * <p>Note: The DataSyncService itself is not notified when a new sync occurs. It's role is to
 * manage the lifecycle of our {@link DataSyncAdapter} and provide a handle to said DataSyncAdapter
 */
public class DataSyncService extends Service {

    private static final String TAG = DataSyncService.class.getCanonicalName();
    private static final Object sSyncAdapterLock = new Object();
    private static DataSyncAdapter sSyncAdapter = null;

    /**
     * Thread-safe constructor, creates static {@link DataSyncAdapter} instance.
     */
    @Override
    public void onCreate() {

        super.onCreate();
        Log.i(TAG, "service created");

        synchronized (sSyncAdapterLock) {

            if (sSyncAdapter == null) {

                sSyncAdapter = new DataSyncAdapter(getApplicationContext(), true);
            }
        }
    }


    public void onDestroy() {

        super.onDestroy();
        Log.i(TAG, "service destroyed");
    }

    /**
     * Return Binder handle for IPC communication with {@link DataSyncAdapter}.
     *
     * <p>New sync requests will be sent directly to the DataSyncAdapter using this channel.
     *
     * @param intent Calling intent
     * @return Binder handle for {@link DataSyncAdapter}
     */
    @Override
    public IBinder onBind(Intent intent) {

        return sSyncAdapter.getSyncAdapterBinder();
    }
}
