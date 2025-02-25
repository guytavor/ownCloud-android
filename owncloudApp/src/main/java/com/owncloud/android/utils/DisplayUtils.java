/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2019 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.format.DateUtils;
import android.view.Display;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;

import java.math.BigDecimal;
import java.net.IDN;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class for some string operations.
 */
public class DisplayUtils {

    private static final String OWNCLOUD_APP_NAME = "ownCloud";

    private static final String[] sizeSuffixes = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private static final int[] sizeScales = {0, 0, 1, 1, 1, 2, 2, 2, 2};

    private static Map<String, String> mimeType2HumanReadable;

    static {
        mimeType2HumanReadable = new HashMap<>();
        // images
        mimeType2HumanReadable.put("image/jpeg", "JPEG image");
        mimeType2HumanReadable.put("image/jpg", "JPEG image");
        mimeType2HumanReadable.put("image/png", "PNG image");
        mimeType2HumanReadable.put("image/bmp", "Bitmap image");
        mimeType2HumanReadable.put("image/gif", "GIF image");
        mimeType2HumanReadable.put("image/svg+xml", "JPEG image");
        mimeType2HumanReadable.put("image/tiff", "TIFF image");
        // music
        mimeType2HumanReadable.put("audio/mpeg", "MP3 music file");
        mimeType2HumanReadable.put("application/ogg", "OGG music file");
    }

    /**
     * Converts the file size in bytes to human readable output.
     * <ul>
     * <li>appends a size suffix, e.g. B, KB, MB etc.</li>
     * <li>rounds the size based on the suffix to 0,1 or 2 decimals</li>
     * </ul>
     *
     * @param bytes Input file size
     * @return Like something readable like "12 MB"
     */
    public static String bytesToHumanReadable(long bytes, Context context) {
        if (bytes < 0) {
            return context.getString(R.string.common_pending);

        } else {
            double result = bytes;
            int attachedSuff = 0;
            while (result >= 1024 && attachedSuff < sizeSuffixes.length) {
                result /= 1024.;
                attachedSuff++;
            }

            BigDecimal readableResult = new BigDecimal(result).setScale(
                    sizeScales[attachedSuff],
                    BigDecimal.ROUND_HALF_UP
            ).stripTrailingZeros();

            // Unscale only values with ten exponent
            return (readableResult.scale() < 0 ?
                    readableResult.setScale(0) :
                    readableResult
            ) + " " + sizeSuffixes[attachedSuff];
        }
    }

    /**
     * Converts MIME types like "image/jpg" to more end user friendly output
     * like "JPG image".
     *
     * @param mimetype MIME type to convert
     * @return A human friendly version of the MIME type
     */
    public static String convertMIMEtoPrettyPrint(String mimetype) {
        if (mimeType2HumanReadable.containsKey(mimetype)) {
            return mimeType2HumanReadable.get(mimetype);
        }
        if (mimetype.split("/").length >= 2) {
            return mimetype.split("/")[1].toUpperCase() + " file";
        }
        return "Unknown type";
    }

    /**
     * Converts Unix time to human readable format
     *
     * @param milliseconds that have passed since 01/01/1970
     * @return The human readable time for the users locale
     */
    public static String unixTimeToHumanReadable(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(date);
    }

    public static int getSeasonalIconId() {
        if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) >= 354 &&
                MainApp.getAppContext().getString(R.string.app_name).equals(OWNCLOUD_APP_NAME)) {
            return R.drawable.winter_holidays_icon;
        } else {
            return R.mipmap.old_icon;
        }
    }

    /**
     * Converts an internationalized domain name (IDN) in an URL to and from ASCII/Unicode.
     *
     * @param url     the URL where the domain name should be converted
     * @param toASCII if true converts from Unicode to ASCII, if false converts from ASCII to Unicode
     * @return the URL containing the converted domain name
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static String convertIdn(String url, boolean toASCII) {

        String urlNoDots = url;
        String dots = "";
        while (urlNoDots.startsWith(".")) {
            urlNoDots = url.substring(1);
            dots = dots + ".";
        }

        // Find host name after '//' or '@'
        int hostStart = 0;
        if (urlNoDots.contains("//")) {
            hostStart = url.indexOf("//") + "//".length();
        } else if (url.contains("@")) {
            hostStart = url.indexOf("@") + "@".length();
        }

        int hostEnd = url.substring(hostStart).indexOf("/");
        // Handle URL which doesn't have a path (path is implicitly '/')
        hostEnd = (hostEnd == -1 ? urlNoDots.length() : hostStart + hostEnd);

        String host = urlNoDots.substring(hostStart, hostEnd);
        host = (toASCII ? IDN.toASCII(host) : IDN.toUnicode(host));

        return dots + urlNoDots.substring(0, hostStart) + host + urlNoDots.substring(hostEnd);
    }

    /**
     * calculates the relative time string based on the given modificaion timestamp.
     *
     * @param context               the app's context
     * @param modificationTimestamp the UNIX timestamp of the file modification time.
     * @return a relative time string
     */
    public static CharSequence getRelativeTimestamp(Context context, long modificationTimestamp) {
        return getRelativeDateTimeString(context, modificationTimestamp, DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS, 0);
    }

    @SuppressWarnings("deprecation")
    public static CharSequence getRelativeDateTimeString(
            Context c, long time, long minResolution, long transitionResolution, int flags
    ) {

        CharSequence dateString;

        // in Future
        if (time > System.currentTimeMillis()) {
            return DisplayUtils.unixTimeToHumanReadable(time);
        }
        // < 60 seconds -> seconds ago
        else if ((System.currentTimeMillis() - time) < 60 * 1000) {
            return c.getString(R.string.file_list_seconds_ago);
        } else {
            dateString = DateUtils.getRelativeDateTimeString(c, time, minResolution, transitionResolution, flags);
        }

        String[] parts = dateString.toString().split(",");
        if (parts.length == 2) {
            if (parts[1].contains(":") && !parts[0].contains(":")) {
                return parts[0];
            } else if (parts[0].contains(":") && !parts[1].contains(":")) {
                return parts[1];
            }
        }
        //dateString contains unexpected format. fallback: use relative date time string from android api as is.
        return dateString.toString();
    }

    /**
     * Update the passed path removing the last "/" if it is not the root folder
     *
     * @param path
     */
    public static String getPathWithoutLastSlash(String path) {

        // Remove last slash from path
        if (path.length() > 1 && path.charAt(path.length() - 1) == OCFile.PATH_SEPARATOR.charAt(0)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Gets the screen size in pixels in a backwards compatible way
     *
     * @param caller Activity calling; needed to get access to the {@link android.view.WindowManager}
     * @return Size in pixels of the screen, or default {@link Point} if caller is null
     */
    public static Point getScreenSize(Activity caller) {
        Point size = new Point();
        if (caller != null) {
            Display display = caller.getWindowManager().getDefaultDisplay();
            display.getSize(size);
        }
        return size;
    }

    /**
     * sets the coloring of the given progress bar to color_accent.
     *
     * @param progressBar the progress bar to be colored
     */
    public static void colorPreLollipopHorizontalProgressBar(ProgressBar progressBar) {
        if (progressBar != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            int color = progressBar.getResources().getColor(R.color.color_accent);
            progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * sets the coloring of the given seek bar to color_accent.
     *
     * @param seekBar the seek bar to be colored
     */
    public static void colorPreLollipopHorizontalSeekBar(SeekBar seekBar) {
        if (seekBar != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            colorPreLollipopHorizontalProgressBar(seekBar);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                int color = seekBar.getResources().getColor(R.color.color_accent);
                seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    /**
     * set the owncloud standard colors for the snackbar.
     *
     * @param context  the context relevant for setting the color according to the context's theme
     * @param snackbar the snackbar to be colored
     */
    public static void colorSnackbar(Context context, Snackbar snackbar) {
        // Changing action button text color
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.white));
    }

    /**
     * Show the avatar corresponding to the received account in an {@ImageView}.
     * <p>
     * The avatar is shown if available locally in {@link ThumbnailsCacheManager}. The avatar is not
     * fetched from the server if not available.
     * <p>
     * If there is no avatar stored, a colored icon is generated with the first letter of the account username.
     * <p>
     * If this is not possible either, a predefined user icon is shown instead.
     *
     * @param account         OC account which avatar will be shown.
     * @param displayView     The image view to set the avatar on.
     * @param displayRadius   The radius of the circle where the avatar will be clipped into.
     * @param fetchFromServer When 'true', if there is no avatar stored in the cache, it's fetched from
     *                        the server. When 'false', server is not accessed, the fallback avatar is
     *                        generated instead. USE WITH CARE, probably to be removed in the future.
     */
    public static void showAccountAvatar(
            Account account,
            ImageView displayView,
            float displayRadius,
            boolean fetchFromServer
    ) {
        if (account != null) {
            // not just accessibility support, used to know what account is bound to each imageView
            displayView.setContentDescription(account.name);

            final ThumbnailsCacheManager.GetAvatarTask task =
                    new ThumbnailsCacheManager.GetAvatarTask(
                            displayView, account, displayRadius, fetchFromServer
                    );
            task.execute();
        }
    }
}
