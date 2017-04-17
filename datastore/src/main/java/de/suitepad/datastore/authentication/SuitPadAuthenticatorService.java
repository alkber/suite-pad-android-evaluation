package de.suitepad.datastore.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Bound Service that instantiates the authenticator when started. This is a stub class meant to
 * satisfy the sync adapter prerequisite. A dummy account is being used here. Actual implementation
 * might have some remote authentication and all.
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */
public class SuitPadAuthenticatorService extends Service {

    private static final String LOGT = SuitPadAuthenticatorService.class.getCanonicalName();
    private static final String ACCOUNT_TYPE = "de.suitpad.datastore";
    public static final String ACCOUNT_NAME = "SuitePadSyncAgent";

    // Instance field that stores the authenticator object
    private Authenticator mAuthenticator;

    public static Account getAssociatedAccount() {

        String accountName = ACCOUNT_NAME;
        return new Account(accountName, ACCOUNT_TYPE);
    }

    @Override
    public void onCreate() {

        mAuthenticator = new Authenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {

        return mAuthenticator.getIBinder();
    }

    public class Authenticator extends AbstractAccountAuthenticator {

        public Authenticator(Context context) {

            super(context);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                     String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                 String s, String s2, String[] strings, Bundle bundle)
                throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                         Account account,
                                         Bundle bundle) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                   Account account,
                                   String s,
                                   Bundle bundle) throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAuthTokenLabel(String s) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                        Account account,
                                        String s,
                                        Bundle bundle) throws NetworkErrorException {

            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                  Account account,
                                  String[] strings) throws NetworkErrorException {

            throw new UnsupportedOperationException();
        }
    }
}
