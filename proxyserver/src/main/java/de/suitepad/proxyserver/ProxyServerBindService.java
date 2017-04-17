package de.suitepad.proxyserver;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import static de.suitepad.proxyserver.ProxyServerServiceCommand.CMD_EMULATE_LOCAL_DATA_AVAILABLE;
import static de.suitepad.proxyserver.ProxyServerServiceCommand.CMD_EMULATE_LOCAL_DATA_EMPTY;
import static de.suitepad.datastore.provider.DataStoreServiceCommand.CMD_START_DATASTORE_SYNC;

/**
 * Bind service of the proxy server. Once a bind callback is made by the frame work it starts the
 * micro web server. Further it also listens of service specific commands.
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */
public class ProxyServerBindService extends Service {

    private static final String LOGT = ProxyServerBindService.class.getCanonicalName();
    private static final int PROXY_SERVER_PORT = 7860;

    private final Object lock = new Object();
    private MicroWebServer webServer;
    private Messenger dataStoreServiceBroker;
    private Messenger messenger = null;
    private boolean dataStoreServiceBrokerBound = false;

    public ProxyServerBindService() {

    }

    /**
     * Listens to command messages for this bind service and executes them.
     */
    private class IncomingMessageHandler extends Handler {

        public IncomingMessageHandler() {

        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case CMD_EMULATE_LOCAL_DATA_EMPTY:

                    if (webServer != null) {

                        webServer.setEmulateDataStoreEmpty(true);
                    }
                    break;
                case  CMD_EMULATE_LOCAL_DATA_AVAILABLE:

                    if (webServer != null) {

                        webServer.setEmulateDataStoreEmpty(false);
                    }
                default:
                    super.handleMessage(msg);
            }
            super.handleMessage(msg);
        }
    }

    private void bindDataStoreService() {

        if(!dataStoreServiceBrokerBound) {

            Intent bindIntend = new Intent();
            bindIntend.setClassName(getString(R.string.data_store_bind_service_package),
                    getString(R.string.data_store_bind_service_qualified_class_name));
            bindService(bindIntend, dataStoreServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindDataStoreService() {

        if (dataStoreServiceBrokerBound) {

            unbindService(dataStoreServiceConnection);
            dataStoreServiceBrokerBound = false;
        }
    }

    /**
     *  Starts the server if it is not running already.
     */
    private void startWebServer() {

        if (webServer == null) {

            webServer = new MicroWebServer(PROXY_SERVER_PORT, getApplicationContext());
            webServer.start();
            Log.v(LOGT, "proxy server started at port " + webServer.getPort());
        } else {

            if (!webServer.isActive()) {

                webServer.start();
            }
        }
    }

    private void stopWebServer() {

        if (webServer != null && webServer.isActive()) {

            webServer.stop();
            Log.v(LOGT, "proxy server stopped at port " + webServer.getPort());
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();
        bindDataStoreService();
    }

    @Override
    public IBinder onBind(Intent intent) {

        synchronized (lock) {

            if (messenger == null) {

                messenger = new Messenger(new IncomingMessageHandler());

            }
        }
        startWebServer();
        return messenger.getBinder();
    }


    @Override
    public void onDestroy() {

        super.onDestroy();

        unbindDataStoreService();
        stopWebServer();
    }

    /**
     * Data store service once installed needs and external trigger to start the sync operation.
     * After installation the first time it is run would setup up data sync service, further calls
     * just validates we have valid states to run the data sync adapter, data sync adapter has been
     * configured to run at certain intervals. Of course, we need to implement boot event listeners
     * if we want to start it after phone restarts.
     */
    private void startDataStoreSyncService() {

        if (!dataStoreServiceBrokerBound) {

            return;
        }

        Message msg = Message.obtain(null, CMD_START_DATASTORE_SYNC, 0, 0);

        try {

            dataStoreServiceBroker.send(msg);
        } catch (RemoteException e) {

            e.printStackTrace();
            Log.e(LOGT, " failed to start data store sync service");
        }
    }

    /**
     * The connection to data sync bind service. Once the connection has been established
     * successfully, it starts the data sync service operation.
     */
    private ServiceConnection dataStoreServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            dataStoreServiceBroker = new Messenger(service);
            dataStoreServiceBrokerBound = true;
            Log.v(LOGT, "bind connection with data store service completed");
            startDataStoreSyncService();
        }

        public void onServiceDisconnected(ComponentName className) {

            dataStoreServiceBroker = null;
            dataStoreServiceBrokerBound = false;
            Log.v(LOGT, "bind connection with data store connection is broken");
        }
    };
}

