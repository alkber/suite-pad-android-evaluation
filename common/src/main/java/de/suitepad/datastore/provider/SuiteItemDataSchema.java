package de.suitepad.datastore.provider;

/**
 * Exposing the table structure of the item info
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */

public class SuiteItemDataSchema {

    public final static int ITEM_DATA_URI_ID = 313;
    public final static String ITEM_DATA_TABLE = "itemInfo";
    public final static String _ID      = "id";
    public final static String _NAME    = "name";
    public final static String _PRICE   = "price";
    public final static String _TYPE    = "type";

    public static String[] getColumns() {

        return new String[] { _ID, _NAME, _PRICE, _TYPE };
    }
}
