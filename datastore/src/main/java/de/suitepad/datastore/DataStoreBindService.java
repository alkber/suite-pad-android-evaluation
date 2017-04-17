package de.suitepad.datastore;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

import de.suitepad.datastore.authentication.SuitPadAuthenticatorService;

/**
 * This is main bind service that would help at initial setup of the data sync service. It should be
 * mostly called from the proxy server service to activate the data sync operation (an initial setup).
 * Since we don't have a launcher activity, this strategy is needed to boot the data sync
 * service the very first first time.
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */
public class DataStoreBindService extends Service {

    private static final String LOGT = DataStoreBindService.class.getCanonicalName();
    private static final int BOOT_DATA_SYNC_SERVICE = 786;
    private static final int SYNC_REPEAT_PERIOD = 60;
    private static final String B_PREF_INITIAL_SETUP_COMPLETE = "initial_setup_complete";
    private Messenger messenger = null;
    private static final Object lock = new Object();

    public DataStoreBindService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOGT, "onBind called ...") ;

        synchronized (lock) {

            if (messenger == null) {
                messenger = new Messenger(new IncomingMessageHandler(getApplicationContext()));
            }
        }
        return messenger.getBinder();
    }

    private static class IncomingMessageHandler extends Handler {

        private  Context context;

        public IncomingMessageHandler(Context context) {

            this.context = context;
        }
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case  BOOT_DATA_SYNC_SERVICE:
                    setupDataSyncService(context);
                    break;
                default:
                    super.handleMessage(msg);
            }
            super.handleMessage(msg);
        }
    }

    /**
     * Called usually first time when when this service is bounded. Once this is executed, further
     * calls to the routine only helps is checking if we are in valid setup for data sync operation.
     * If not, a fresh setup is done once again. For simplicity sake we are only using a stub account.
     *
     * @param context
     */
    private static void setupDataSyncService(Context context) {

        String CONTENT_AUTHORITY = context.getString(R.string.content_authority);

        boolean freshAccount = false;
        /* This will check to see if such an account already exist, if not create one and configure
         * sync policies.
         */
        Account account = SuitPadAuthenticatorService.getAssociatedAccount();
        AccountManager accountManager = (AccountManager)
                context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {

            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY,
                    new Bundle(),SYNC_REPEAT_PERIOD);
            freshAccount = true;
            Log.v(LOGT, "new sync account created successfully");
        }

        boolean initialSetupComplete = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(B_PREF_INITIAL_SETUP_COMPLETE, false);

        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (freshAccount || !initialSetupComplete) {

            Log.v(LOGT, "attempting a manual sync");
            startManualSync(context);
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(B_PREF_INITIAL_SETUP_COMPLETE, true).commit();
        }
    }

    /**
     * Does a manual sync operation, usually called the first time when account is setup. The
     * routine assumes a valid account has been already setup.
     *
     * @param context
     */
    private static void startManualSync(Context context) {

        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                SuitPadAuthenticatorService.getAssociatedAccount(),
                context.getString(R.string.content_authority),
                b);
    }

}
