package de.suitepad.presentation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import de.suitepad.presentation.common.PackageHelper;
import de.suitepad.proxyserver.ProxyServerServiceCommand;

/**
 * Main presentation activity as per the specification. This activity is light weight and is only
 * responsible functionally to load web page from the asset folder. However, apart from the given
 * functionality one more was added to toggle between local and remote data availability. Since
 * data is always available from the data store, some means was required to emulate no data
 * condition. This is however triggered by toggle button, which communicates via messages to the
 * proxy server bind service.
 *
 * Althaf K Backer (althafkbacker@gmail.com> April 2017
 */
public class SuitePadFrontEndActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOGT = SuitePadFrontEndActivity.class.getSimpleName();
    /* message broker that communicates with the proxy server service */
    private Messenger proxyServerServiceBroker = null;
    /* flag to know if proxy server is bound to this activity */
    private boolean isProxyServerServiceBound = false;
    /* <code> isProxyServerServiceBound </code> is updated via this lock */
    private Object boundUpdateLock = new Object();
    /* main web view */
    private WebView webView;
    /* button to toggle emulation data availability */
    private Button btnModeToggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suite_pad_front_end);

        bindProxyServer();

        webView = (WebView) findViewById(R.id.webview);

        btnModeToggle = (Button) findViewById(R.id.toogle_button);
        btnModeToggle.setOnClickListener(this);

        /* so that json can be rendered to html tables via the script */
        if (!webView.getSettings().getJavaScriptEnabled()) {

            webView.getSettings().setJavaScriptEnabled(true);
            Log.i(LOGT, "enabling java script support");
        }
    }

    /**
     * Bound connection to the proxy server service. When the connection is successful, it loads the
     * the home page.
     */
    private ServiceConnection proxyServerBoundConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            synchronized (boundUpdateLock) {

                proxyServerServiceBroker = new Messenger(service);
                isProxyServerServiceBound = true;
            }

            Log.v(LOGT, "attempt to bind proxy server successful");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                  loadHomePage();
                }
            });
        }

        /*
         This is called when the connection with the service has been
         unexpectedly disconnected -- that is, its process crashed
         */
        public void onServiceDisconnected(ComponentName className) {

            Log.w(LOGT, "lost bind connection to proxy server");

            synchronized (boundUpdateLock) {

                proxyServerServiceBroker = null;
                isProxyServerServiceBound = false;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    webView.loadUrl(getString(R.string.lost_connection_page_path));
                }
            });
        }
    };

    @Override
    protected void onResume() {

        super.onResume();
        loadHomePage();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unbindProxyServer();
    }

    private void bindProxyServer() {

        if (!isProxyServerServiceBound) {

            Log.v(LOGT, "attempting to bind proxy server");
            Intent bindIntent = new Intent();
            bindIntent.setClassName(
                    getString(R.string.proxy_server_package),
                    getString(R.string.proxy_server_qualified_class_name));
            bindService(bindIntent, proxyServerBoundConnection, Context.BIND_AUTO_CREATE);
        } else {

            Log.w(LOGT, "proxy server is already bound");
        }
    }

    private void unbindProxyServer() {

        synchronized (boundUpdateLock) {

            if (isProxyServerServiceBound) {

                unbindService(proxyServerBoundConnection);
                isProxyServerServiceBound = false;
                Log.v(LOGT, "unbinding from proxy server complete");
            }
        }
    }

    @Override
    public void onClick(View view) {

        if (view == btnModeToggle) {

            try {

                boolean reloadWebView = onToggleButtonClick(btnModeToggle.getText().toString());

                if (reloadWebView) {

                    btnModeToggle.setEnabled(false);

                    Toast.makeText(getApplicationContext(),
                            getString(R.string.msg_please_wait_while),
                            Toast.LENGTH_LONG).show();

                    webView.loadUrl(getString(R.string.home_page_path));
                    btnModeToggle.setEnabled(true);
                }

            } catch (RemoteException e) {

                Toast.makeText(getApplicationContext(), getString(R.string.msg_button_toggle_failed)
                        + e.toString(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    /**
     * Toggles the availability of the data in the data store service. Main intention of this
     * implementation is to test the functional aspect of the application, ie how it would behave
     * when local data is not available. As for our demo purpose, local data is already loaded by
     * the data store service from its application assets, we don't have a realistic scenario for
     * lack of local data. So this functionality would emulate that. This is done by sending message
     * to bound proxy server service.
     *
     * @param currentButtonText toggling is based on current text on the button
     * @return      <code>true</code> if toggle was successful
     * @throws RemoteException
     */
    private boolean onToggleButtonClick(String currentButtonText) throws RemoteException {

        boolean reloadPage = false;

        if (isProxyServerServiceBound && proxyServerServiceBroker != null) {

            /* done so that another on click event is not triggered until we are complete */
            btnModeToggle.setEnabled(false);

            if (getString(R.string.toogle_button_local)
                    .equals(currentButtonText)) {

                reloadPage = true;
                sendCommandReqToProxyServer(ProxyServerServiceCommand.CMD_EMULATE_LOCAL_DATA_AVAILABLE);
                btnModeToggle.setText(getString(R.string.toogle_button_remote));
            } else if (getString(R.string.toogle_button_remote)
                    .equals(currentButtonText)) {

                reloadPage = true;
                sendCommandReqToProxyServer(ProxyServerServiceCommand.CMD_EMULATE_LOCAL_DATA_EMPTY);
                btnModeToggle.setText(getString(R.string.toogle_button_local));
            }
        }

        return reloadPage;
    }

    /**
     * Send command request to proxy server service
     * @param command any list of commands from <code>{@link ProxyServerServiceCommand}</code>
     * @throws RemoteException
     */
    private void sendCommandReqToProxyServer(int command) throws RemoteException {

        if (isProxyServerServiceBound && proxyServerServiceBroker != null) {

            Message msg = Message.obtain(null, command, 0, 0);
            proxyServerServiceBroker.send(msg);
        }
    }

    /**
     * Loads the home page with some prerequisite checks. Usually run the first time the application
     * is launched. It verifies if the dependency services required are already installed.
     */
    private void loadHomePage() {

        if (webView != null) {

            boolean isProxyServerInstalled = PackageHelper.
                    isAppInstalled(getApplicationContext(), getString(R.string.proxy_server_package));
            boolean isDataStoreServiceInstalled = PackageHelper.
                    isAppInstalled(getApplicationContext(), getString(R.string.data_store_package));

            /* if either of them is message user is requested to install them */
            if (!isProxyServerInstalled || !isDataStoreServiceInstalled) {

                webView.loadUrl(getString(R.string.ensure_dependency_page_path));
                btnModeToggle.setEnabled(false);
                return;
            }

            /* we have to use this lock as the <code> ServiceConnection </code> asynchronous callbacks
               also updates <code>isProxyServerBound</code>. The bind operation can take a little
               while.
             */
            synchronized (boundUpdateLock) {

                if (!isProxyServerServiceBound) {

                    webView.loadUrl(getString(R.string.init_page_path));
                } else {

                    webView.loadUrl(getString(R.string.home_page_path));
                }
            }
        }
    }
}
