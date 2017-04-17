package de.suitepad.proxyserver;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import de.suitepad.datastore.provider.SuiteItemDataSchema;
import de.suitepad.helper.FileHelper;
import de.suitepad.helper.ResourceHelper;

/**
 * Basic HTTP server, that handles one request at a time (next caller would be blocked,
 * until first completes).Supports GET method only.
 * <p>
 * If the data is found from the data store content provider then that cache data is given as the
 * response to the caller, else the request is forwarded to a remote server and the response data
 * from the remote server is is given as response to the local caller.
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */
public class MicroWebServer implements Runnable {

    private static final String LOGT = MicroWebServer.class.getCanonicalName();
    /* port at which server listen to*/
    private final int listingPort;
    /* flag used to know if server is running now or not*/
    private boolean serverActive;
    /* main entry point socket */
    private ServerSocket serverSocket;
    /* application context , required to get content provider*/
    private Context context;

    /* set to true for emulating "data store has no data" condition */
    private boolean emulateDataStoreEmpty = false;


    /**
     * Initialize with port at which server would listen to. <code>Context</code> is required to
     * interact with the content provider.
     *
     * @param port
     * @param context
     */
    public MicroWebServer(int port, Context context) {

        listingPort = port;
        this.context = context;
    }

    /**
     * Start the server if it is already not running
     */
    public void start() {

        if (!serverActive) {

            serverActive = true;
            new Thread(this).start();
        }
    }

    /**
     * Stop the server
     */
    public void stop() {

        try {

            serverActive = false;
            if (serverSocket != null) {

                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    /**
     * Get the current server running status
     *
     * @return
     */

    public boolean isActive() {

        return serverActive;
    }

    /**
     * Get the port at which server is actively listening to.
     *
     * @return
     */
    public int getPort() {

        return listingPort;
    }

    /**
     * This enabling this flag would ensure that loading data from data store would always empty.
     * This would help load the data from the remote server. Even though data is present in the data
     * store.This demo purpose, Though it violates the policy of separation of concerns.
     *
     * @param enable  <code>true</code> would emulate data store empty condition even if data
     *                                  is available
     *                <code>false</code> would disable data store empty condition
     */
    public void setEmulateDataStoreEmpty(boolean enable) {

        emulateDataStoreEmpty = enable;
    }

    /**
     * Main heart of the server, listens for the request at <code> listingPort </code> and
     * dispatches the request is first come order. It is not a multi threaded request handler
     * next caller would be blocked until the request of the first caller is complete. This is
     * unlike the normal web servers
     */
    @Override
    public void run() {

        try {

            serverSocket = new ServerSocket(listingPort);

            while (serverActive) {

                if (serverSocket != null && !serverSocket.isClosed()) {

                    Socket socket = serverSocket.accept();
                    handle(socket);
                    socket.close();
                }
            }
        } catch (SocketException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    /**
     * The request handler. Currently only supports the http GET operation. First it gets the route
     * from the GET request. Only intention of getting the route is to set the mime part when server
     * responds back.Currently it whatever may be the route, it just responds back with the same data.
     *
     * Behaviour of the <code>handle()</code> changes based on the flag
     * <code> emulateDataStoreEmpty </code> . Which triggers, emulation of the cache miss from local
     * data store. If this mode is enabled it fetches the data from the remote server and serve this
     * back the the caller. Of course, it assumes there is a valid internet connectivity. This mode
     * can be toggled by sending <code> {@link ProxyServerServiceCommand}</code> to the bind proxy
     * server service.
     *
     * This mode is entirely for demo purpose, since data is already available to the data store
     * from the assets folder, some method was required to emulate lack of data.
     *
     * @param socket
     * @throws IOException
     */
    private void handle(Socket socket) throws IOException {

        PrintStream responseOutputStream = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        try {

            String GETroute = retrieveGETroute(reader);
            responseOutputStream = new PrintStream(socket.getOutputStream());

            if (GETroute == null) {

                writeInternalServerError(responseOutputStream);
                return;
            }

            /*
            * may be we could do some validation of the route here, currently what ever be the
            * route, it just respond back with what ever data it has cached.
            **/

            byte[] bytes = loadDataFromDataStore();
            /**
             * If bytes[] is empty it could either mean local cache is empty or emulation mode is
             * enabled. Now it tries to fetch from the remote url.
             */
            if (bytes == null) {

                bytes = loadDataFromRemote(context.getString(R.string.remote_url));
            }

            if (bytes == null) {

                writeInternalServerError(responseOutputStream);
                return;
            }

            responseOutputStream.println("HTTP/1.0 200 OK");
            responseOutputStream.println("Content-Type: " + FileHelper.mime(GETroute));
            responseOutputStream.println("Content-Length: " + bytes.length);
            responseOutputStream.println("Access-Control-Allow-Origin: " + "null");
            responseOutputStream.println("Access-Control-Allow-Methods: " + "GET");

            responseOutputStream.println();
            responseOutputStream.write(bytes);
            responseOutputStream.flush();


        } finally {

            ResourceHelper.close(responseOutputStream);
            ResourceHelper.close(reader);
         }
    }

    private void writeInternalServerError(PrintStream output) {

        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }

    private void writeRedirected(PrintStream output, String url) {

        output.println("HTTP/1.0 307 Move Temporally");
        output.println("Access-Control-Allow-Origin: " + "*");
        output.println("Location: " + url);
        output.flush();
    }

    /**
     * For demo sake we are trusting all https connection
     */
    private byte[] loadDataFromRemote(String remoteHttpUrl) {

        URLConnection urlConnection = null;
        InputStream in = null;
        ByteArrayOutputStream output = null;

        try {

            URL url = new URL(remoteHttpUrl);
            urlConnection = url.openConnection();

            if (urlConnection instanceof HttpsURLConnection) {

                HttpsURLConnection https = (HttpsURLConnection) urlConnection;

                https.setHostnameVerifier(new HostnameVerifier() {

                    @Override
                    public boolean verify(String s, SSLSession sslSession) {

                        return true;
                    }
                });
            }

            in = new BufferedInputStream(urlConnection.getInputStream());
            output = new ByteArrayOutputStream();

            byte[] buffer = new byte[2048];
            int size;

            while (-1 != (size = in.read(buffer))) {

                output.write(buffer, 0, size);
            }

            Log.v(LOGT, new String(output.toByteArray()));

        } catch (IOException ioe) {

            ioe.printStackTrace();
        } finally {

            ResourceHelper.close(output);
            ResourceHelper.close(in);
        }

        return output == null ? null : output.toByteArray();
    }

    /**
     * Load data from the data store , serialize it to json array.
     * For the demo purpose, data store empty condition can be emulated by setting
     * <code> emulateDataStoreEmpty </code> to true.
     *
     * @return <code>byte[]</code> of json string if they are found in the data store
     *         <code>null</code> if nothing is found
     */
    private byte[] loadDataFromDataStore() {

        if(emulateDataStoreEmpty) {

            return null;
        }

        final String URI = context.getString(R.string.data_store_provider_uri) +
                SuiteItemDataSchema.ITEM_DATA_TABLE;

        Cursor cursor = context.getContentResolver().query(
                Uri.parse(URI),
                SuiteItemDataSchema.getColumns(),
                null,
                null,
                null);

        if (cursor != null) {

            JSONArray jsonArray = new JSONArray();

            try {

                while (cursor.moveToNext()) {

                    JSONObject json = new JSONObject();
                    json.put(SuiteItemDataSchema._ID,
                            cursor.getString(cursor.getColumnIndex(SuiteItemDataSchema._ID)));
                    json.put(SuiteItemDataSchema._NAME,
                            cursor.getString(cursor.getColumnIndex(SuiteItemDataSchema._NAME)));
                    json.put(SuiteItemDataSchema._TYPE,
                            cursor.getString(cursor.getColumnIndex(SuiteItemDataSchema._TYPE)));
                    json.put(SuiteItemDataSchema._PRICE,
                            cursor.getString(cursor.getColumnIndex(SuiteItemDataSchema._PRICE)));

                    jsonArray.put(json);
                }

            } catch (JSONException je) {

                je.printStackTrace();
                return null;
            }

            return jsonArray.toString().getBytes();
        }

        return null;
    }

    /**
     * Extracts the route part after the GET request.
     *
     * @param reader
     * @return
     * @throws IOException
     */
    private String retrieveGETroute(BufferedReader reader) throws IOException {

        String route = null;
        String currentLine;

        while (!TextUtils.isEmpty(currentLine = reader.readLine())) {

            Log.v(LOGT, currentLine);
            if (currentLine.startsWith("GET /")) {

                int start = currentLine.indexOf('/') + 1;
                int end = currentLine.indexOf(' ', start);

                route = currentLine.substring(start, end);

                Log.v(LOGT, route);

                break;
            }
        }

        return route;
    }
}
