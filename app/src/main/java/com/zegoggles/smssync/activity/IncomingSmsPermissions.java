package com.zegoggles.smssync.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.zegoggles.smssync.R;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.RECEIVE_MMS;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Runtime permissions needed for automatic backup: read the inbox and receive
 * SMS/MMS broadcasts so {@link com.zegoggles.smssync.receiver.SmsBroadcastReceiver}
 * can schedule incoming backups.
 *
 * After a Deny (especially “Don’t ask again” / OEM one-shot denials),
 * {@link ActivityCompat#requestPermissions} may not show a system dialog again;
 * in that case we send the user to app settings.
 */
public final class IncomingSmsPermissions {
    /** {@link ActivityCompat#requestPermissions} request code. */
    public static final int REQUEST_CODE = 7;

    private static final String PREF_SMS_PERMISSIONS_REQUESTED = "sms_runtime_permissions_requested";

    private IncomingSmsPermissions() {}

    public static String[] required() {
        return new String[]{READ_SMS, RECEIVE_SMS, RECEIVE_MMS};
    }

    @NonNull
    public static String[] missing(@NonNull Context context) {
        List<String> missing = new ArrayList<String>(3);
        for (String permission : required()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        return missing.toArray(new String[0]);
    }

    /**
     * Explains why SMS access is needed, then shows the system permission dialog
     * or opens app settings when Android will no longer prompt.
     */
    public static void requestWithRationale(@NonNull final Activity activity) {
        requestWithRationale(activity, missing(activity), REQUEST_CODE);
    }

    /**
     * Same as {@link #requestWithRationale(Activity)} but for an arbitrary
     * permission set / request code (e.g. backup-service notification path).
     */
    public static void requestWithRationale(@NonNull final Activity activity,
                                            @NonNull final String[] permissions,
                                            final int requestCode) {
        if (permissions.length == 0) {
            return;
        }
        final boolean openSettings = !canShowSystemPrompt(activity, permissions);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.ui_dialog_incoming_sms_permission_title)
                .setMessage(openSettings
                        ? R.string.ui_dialog_incoming_sms_permission_settings_msg
                        : R.string.ui_dialog_incoming_sms_permission_msg)
                .setPositiveButton(
                        openSettings
                                ? R.string.ui_dialog_open_app_settings
                                : android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (openSettings) {
                                    openApplicationDetailsSettings(activity);
                                } else {
                                    markSmsPermissionsRequested(activity);
                                    ActivityCompat.requestPermissions(activity, permissions, requestCode);
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static boolean canShowSystemPrompt(@NonNull Activity activity, @NonNull String[] permissions) {
        if (!wasSmsPermissionsRequested(activity)) {
            // First ask: rationale is false, but the system dialog still appears.
            return true;
        }
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    static void markSmsPermissionsRequested(@NonNull Context context) {
        prefs(context).edit().putBoolean(PREF_SMS_PERMISSIONS_REQUESTED, true).apply();
    }

    static boolean wasSmsPermissionsRequested(@NonNull Context context) {
        return prefs(context).getBoolean(PREF_SMS_PERMISSIONS_REQUESTED, false);
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static void openApplicationDetailsSettings(@NonNull Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }
}
