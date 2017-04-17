package de.suitepad.datastore.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.suitepad.datastore.R;
import de.suitepad.helper.ResourceHelper;

import static de.suitepad.datastore.provider.SuiteItemDataSchema.ITEM_DATA_TABLE;
import static de.suitepad.datastore.provider.SuiteItemDataSchema.ITEM_DATA_URI_ID;

/**
 * Data provider of the application.
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */
public class DataProvider extends ContentProvider {

    private static final String LOGT = DataProvider.class.getCanonicalName();
    private static final String AUTHORITY = "de.suitepad.datastore.provider";
    private MatrixCursor emulatedData;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static  {

        sUriMatcher.addURI(AUTHORITY, ITEM_DATA_TABLE, ITEM_DATA_URI_ID);
    }


    @Override
    public boolean onCreate() {

        emulatedData = (MatrixCursor) emulatedDataCursor();
        return true;
    }

    /**
     * Determine the mime type for entries returned by a given URI.
     */
    @Override
    public String getType(Uri uri) {

        final int match = 0;

        switch (match) {
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Perform a database query by URI.
     **/
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        Log.v(LOGT, "received uri" + uri);
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {

            case ITEM_DATA_URI_ID:
                return emulatedDataCursor();
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Insert a new entry into the database.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final int match = 0;
        switch (match) {
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Delete an entry by database by URI.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        final int match = 0;
        switch (match) {
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Update an etry in the database by URI.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        final int match = 0;
        switch (match) {
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
          }

    /**
     * The emulated cursor is stub implementation of the actual data. Current read only storage is
     * is the data.json file from the assets folder. Practical implementation would have sqlite
     * light caching the data. It reads the json file from the assets folder add each row to the
     * cursor. It only does this once during the provider runtime. Next call returns the cursor
     * which was already set up.
     *
     * @return <code>Cursor</code> of emulated data a <code>MatrixCursor</oode>
     */
    private Cursor emulatedDataCursor() {

        if(emulatedData == null) {

            emulatedData = new MatrixCursor(SuiteItemDataSchema.getColumns(), 25);
        } else {

            return  emulatedData;
        }

        try {

            JSONTokener jt = new JSONTokener(loadContent(getContext().getString(R.string.data_file)));
            JSONArray jArray = new JSONArray(jt);

            for(int pos = 0; pos < jArray.length(); pos++) {

                String[] currentRow = new String[4];
                currentRow[0] = jArray.getJSONObject(pos).getString(SuiteItemDataSchema._ID);
                currentRow[1] = jArray.getJSONObject(pos).getString(SuiteItemDataSchema._NAME);
                currentRow[2] = jArray.getJSONObject(pos).getString(SuiteItemDataSchema._PRICE);
                currentRow[3] = jArray.getJSONObject(pos).getString(SuiteItemDataSchema._TYPE);
                emulatedData.addRow(currentRow);
            }
        } catch (JSONException je) {

            je.printStackTrace();
        } catch (IOException ioe) {

            ioe.printStackTrace();
        }

        return  emulatedData;
    }

    /**
     * Load the a text file from the assets folder and returns it string representation.
     *
     * @param fileName relative path of the file in the assets folder
     * @return  string representation of the file
     * @throws IOException
     */
    private String loadContent(String fileName) throws IOException {

        InputStream input = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {

            input = getContext().getAssets().open(fileName);

            byte[] buffer = new byte[2048];
            int size;

            while (-1 != (size = input.read(buffer))) {

                output.write(buffer, 0, size);
            }

            return new String(output.toByteArray());

        } catch (FileNotFoundException e) {

            return null;
        } finally {

            output.flush();
            ResourceHelper.close(input);
        }
    }
}
