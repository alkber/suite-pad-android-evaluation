package de.suitepad.presentation.common;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 *
 * Althaf K Backer (althafkbacker@gmail.com) April 2017
 */
public class PackageHelper {

    public static boolean isAppInstalled(Context context, String packageName) {

        try {

            context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {

            return false;
        }
    }
}
