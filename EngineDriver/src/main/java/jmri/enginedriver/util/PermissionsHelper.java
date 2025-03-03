package jmri.enginedriver.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jmri.enginedriver.R;

public class PermissionsHelper {

    /**
     * A compile time annotation to range-check the list of possible permission request codes.
     * Implemented this way as an Enum is 'heavier' and this checking is only really needed for
     * internal reasons.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
//            CLEAR_CONNECTION_LIST,
//            READ_CONNECTION_LIST,
//            STORE_CONNECTION_LIST,
            READ_PHONE_STATE,
//            READ_PREFERENCES,
//            STORE_PREFERENCES,
//            READ_FUNCTION_SETTINGS,
//            STORE_FUNCTION_SETTINGS,
//            STORE_LOG_FILES,
            CONNECT_TO_SERVER,
            WRITE_SETTINGS,
            ACCESS_FINE_LOCATION,
//            STORE_SERVER_AUTO_PREFERENCES,
//            READ_SERVER_AUTO_PREFERENCES,
            VIBRATE,
            READ_IMAGES,
//            READ_LEGACY_FILES,
//            NEARBY_WIFI_DEVICES,
            READ_MEDIA_IMAGES,
            READ_MEDIA_VISUAL_USER_SELECTED,
            POST_NOTIFICATIONS,
            ACCESS_COARSE_LOCATION,
            ACCESS_WIFI_STATE,
            INTERNET
    })
    public @interface RequestCodes {}

    /**
     * List of possible permission request codes
     */
//    public static final int CLEAR_CONNECTION_LIST = 32;
//    public static final int READ_CONNECTION_LIST = 33;
//    public static final int STORE_CONNECTION_LIST = 34;
    public static final int READ_PHONE_STATE = 35;
//    public static final int READ_PREFERENCES = 36;
//    public static final int STORE_PREFERENCES = 37;
//    public static final int READ_FUNCTION_SETTINGS = 38;
//    public static final int STORE_FUNCTION_SETTINGS = 39;
    public static final int CONNECT_TO_SERVER = 40;
    public static final int WRITE_SETTINGS = 41;
    public static final int ACCESS_FINE_LOCATION = 42;
//    public static final int STORE_SERVER_AUTO_PREFERENCES = 43;
//    public static final int READ_SERVER_AUTO_PREFERENCES = 44;
//    public static final int STORE_LOG_FILES = 45;
    public static final int VIBRATE = 46;
    public static final int READ_IMAGES = 47;
//    public static final int READ_LEGACY_FILES = 48;

//<!-- needed for API 33 -->
//    public static final int NEARBY_WIFI_DEVICES = 49;
    public static final int READ_MEDIA_IMAGES = 50;
    public static final int POST_NOTIFICATIONS = 51;
//<!-- needed for API 33 -->
//<!-- needed for API 34 -->
    public static final int READ_MEDIA_VISUAL_USER_SELECTED = 52;
//<!-- needed for API 34 -->

    public static final int ACCESS_COARSE_LOCATION = 53;
    public static final int ACCESS_WIFI_STATE = 54;
    public static final int INTERNET = 55;

    private boolean isDialogOpen = false;
    private static PermissionsHelper instance = null;

    /**
     * Ensures only one instance of this helper exists
     *
     * @return the current instance, or a new one if not yet instantiated
     */
    public static PermissionsHelper getInstance() {
        if (instance == null) {
            instance = new PermissionsHelper();
        }
        return instance;
    }

    /**
     * Process the request permission results
     *
     * @param activity the requesting Activity
     * @param requestCode the permissions request code
     * @param permissions the permissions array
     * @param grantResults the results array
     * @return true if recognised permissions request
     */
    public boolean processRequestPermissionsResult(final Activity activity, @RequestCodes int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        boolean isRecognised = false;

        // process the resultCode array
        for (int i=0; i<permissions.length; i++) {
            int grantResult = grantResults[i];

            if (!showPermissionRationale(activity, requestCode) && grantResult != PackageManager.PERMISSION_GRANTED) {
                isRecognised = true;
                Log.d("Engine_Driver", "Permission denied - showAppSettingsDialog");
                showAppSettingsDialog(activity, requestCode);
                break;
            } else if (grantResult != PackageManager.PERMISSION_GRANTED) {
                isRecognised = true;
                Log.d("Engine_Driver", "Permission denied - showRetryDialog");
                showRetryDialog(activity, requestCode);
                break;
            } else {
                isRecognised = true;
                Log.d("Engine_Driver", "Permission granted - navigateToHandler");
                ((PermissionsHelperGrantedCallback) activity).navigateToHandler(requestCode);
            }
        }

        //context.navigateToHandler(requestCode, resultCode);
        return isRecognised;
    }

    /**
     * Internal method to retrieve the appropriate message for retry and rationale dialogs
     *
     * @param context the requesting Activity's context
     * @param requestCode the permissions request code
     * @return message
     */
    private String getMessage(final Context context, @RequestCodes final int requestCode) {
        // Get the relevant rationale message based on request code
        // All possible request codes should be considered
        switch (requestCode) {
//            case READ_CONNECTION_LIST:
//                return context.getResources().getString(R.string.permissionsReadRecentConnections);
//            case CLEAR_CONNECTION_LIST:
//                return context.getResources().getString(R.string.permissionsClearRecentConnections);
//            case STORE_CONNECTION_LIST:
//                return context.getResources().getString(R.string.permissionsStoreRecentConnections);
//            case READ_PREFERENCES:
//                return context.getResources().getString(R.string.permissionsReadPreferences);
            case READ_IMAGES:
                return context.getResources().getString(R.string.permissionsREAD_IMAGES);
//            case READ_LEGACY_FILES:
//                return context.getResources().getString(R.string.permissionsReadLegacyFiles);
//            case STORE_PREFERENCES:
//            case STORE_SERVER_AUTO_PREFERENCES:
//            case READ_SERVER_AUTO_PREFERENCES:
//            case STORE_LOG_FILES:
//                return context.getResources().getString(R.string.permissionsStorePreferences);
            case READ_PHONE_STATE:
                return context.getResources().getString(R.string.permissionsReadPhoneState);
//            case STORE_FUNCTION_SETTINGS:
//                return context.getResources().getString(R.string.permissionsStoreFunctionSettings);
//            case READ_FUNCTION_SETTINGS:
//                return context.getResources().getString(R.string.permissionsReadFunctionSettings);
            case CONNECT_TO_SERVER:
                return context.getResources().getString(R.string.permissionsConnectToServer);
            case WRITE_SETTINGS:
                return context.getResources().getString(R.string.permissionsWriteSettings);
            case ACCESS_FINE_LOCATION:
                return context.getResources().getString(R.string.permissionsACCESS_FINE_LOCATION);
            case VIBRATE:
                return context.getResources().getString(R.string.permissionsVIBRATE);
            case READ_MEDIA_IMAGES: // needed for API 33
                return context.getResources().getString(R.string.permissionsREAD_MEDIA_IMAGES);
            case READ_MEDIA_VISUAL_USER_SELECTED: // needed for API 34
                return context.getResources().getString(R.string.permissionsREAD_MEDIA_VISUAL_USER_SELECTED);
//            case NEARBY_WIFI_DEVICES:
//                return context.getResources().getString(R.string.permissionsNEARBY_WIFI_DEVICES);
            case POST_NOTIFICATIONS:
                return context.getResources().getString(R.string.permissionsPOST_NOTIFICATIONS);
//<!-- needed for API 33 -->

            case ACCESS_COARSE_LOCATION:
                return context.getResources().getString(R.string.permissionsACCESS_COARSE_LOCATION);
            case ACCESS_WIFI_STATE:
                return context.getResources().getString(R.string.permissionsACCESS_WIFI_STATE);
            case INTERNET:
                return context.getResources().getString(R.string.permissionsINTERNET);

            default:
                return "Unknown permission request: " + requestCode;
        }
    }

    /**
     * Method to request the necessary permissions
     *
     * @param activity the requesting Activity
     * @param requestCode the permissions request code
     */
    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestNecessaryPermissions(final Activity activity, @RequestCodes final int requestCode) {
        // Request the necessary permissions based on request code
        // All possible request codes should be considered
        Log.d("Engine_Driver", "isDialogOpen at requestNecessaryPermissions? " + isDialogOpen);
        if (!isDialogOpen) {
            switch (requestCode) {
//                case READ_CONNECTION_LIST:
//                case CLEAR_CONNECTION_LIST:
//                case STORE_CONNECTION_LIST:
//                case READ_PREFERENCES:
//                case STORE_PREFERENCES:
//                case STORE_FUNCTION_SETTINGS:
//                case STORE_LOG_FILES:
//                case READ_FUNCTION_SETTINGS:
//                case STORE_SERVER_AUTO_PREFERENCES:
//                case READ_SERVER_AUTO_PREFERENCES:
//                    Log.d("Engine_Driver", "Requesting STORAGE permissions");
//                    activity.requestPermissions(new String[]{
//                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                    Manifest.permission.READ_EXTERNAL_STORAGE},
//                            requestCode);
//                    break;
//                case READ_LEGACY_FILES:
                case READ_IMAGES:
                    Log.d("Engine_Driver", "Requesting STORAGE permissions");
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            requestCode);
//                    break;
                case READ_PHONE_STATE:
                    Log.d("Engine_Driver", "Requesting PHONE permissions");
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.READ_PHONE_STATE},
                            requestCode);
                    break;
                case ACCESS_FINE_LOCATION:
                    Log.d("Engine_Driver", "Requesting ACCESS_FINE_LOCATION permissions");
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            requestCode);
                    break;
                case CONNECT_TO_SERVER:
//                    Log.d("Engine_Driver", "Requesting PHONE and STORAGE permissions");
                    Log.d("Engine_Driver", "Requesting PHONE permission");
//                    activity.requestPermissions(new String[]{
//                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    requestCode);
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.READ_PHONE_STATE},
                            requestCode);
                    break;
                case WRITE_SETTINGS:
                    Log.d("Engine_Driver", "Requesting WRITE_SETTINGS permissions");
                    if (Build.VERSION.SDK_INT < 23) {
                        activity.requestPermissions(new String[]{
                                        Manifest.permission.WRITE_SETTINGS},
                                requestCode);
                    } else {
                        if (!Settings.System.canWrite(activity)) {
                            showAppSettingsDialog(activity, requestCode);
                        }
                    }
                    break;
                case VIBRATE:
                    Log.d("Engine_Driver", "Requesting VIBRATE permissions");
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.VIBRATE},
                            requestCode);
                    break;

                case READ_MEDIA_IMAGES: // needed for API 33
                    Log.d("Engine_Driver", "Requesting READ_MEDIA_IMAGES permissions");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity.requestPermissions(new String[]{
                                        Manifest.permission.READ_MEDIA_IMAGES},
                                requestCode);
                    }
                    break;
                case READ_MEDIA_VISUAL_USER_SELECTED: // needed for API 34
                    Log.d("Engine_Driver", "Requesting READ_MEDIA_VISUAL_USER_SELECTED permissions");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        activity.requestPermissions(new String[]{
                                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED},
                                requestCode);
                    }
                    break;
//                case NEARBY_WIFI_DEVICES:
//                    activity.requestPermissions(new String[]{
//                                    Manifest.permission.NEARBY_WIFI_DEVICES},
//                            requestCode);
//                    Log.d("Engine_Driver", "Requesting NEARBY_WIFI_DEVICES permissions");
//                    break;
                case POST_NOTIFICATIONS:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity.requestPermissions(new String[]{
                                        Manifest.permission.POST_NOTIFICATIONS},
                                requestCode);
                    }
                    Log.d("Engine_Driver", "Requesting POST_NOTIFICATIONS permissions");
                    break;
//<!-- needed for API 33 -->

                case ACCESS_COARSE_LOCATION:
                    Log.d("Engine_Driver", "Requesting ACCESS_FINE_LOCATION permissions");
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION},
                            requestCode);
                    break;
                case ACCESS_WIFI_STATE:
                    Log.d("Engine_Driver", "Requesting ACCESS_WIFI_STATE permissions");
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.ACCESS_WIFI_STATE},
                            requestCode);
                    break;
                case INTERNET:
                    Log.d("Engine_Driver", "Requesting INTERNET permissions");
                    activity.requestPermissions(new String[]{
                                    Manifest.permission.INTERNET},
                            requestCode);
                    break;


            }
        } else {
            Log.d("Engine_Driver", "Permissions dialog is opened - don't ask yet...");
        }
    }

    /**
     * Internal method to display a link to the application settings when permissions request denied
     * and a retry is no longer possible
     *
     * @param context the requesting Activity's context
     * @param requestCode the permissions request code
     */
    private void showAppSettingsDialog(final Context context, @RequestCodes final int requestCode) {
        String positiveButtonLabel;
        if (requestCode != WRITE_SETTINGS) {
            positiveButtonLabel = context.getResources().getString(R.string.permissionsAppSettingsButton);
        } else {
            positiveButtonLabel = context.getResources().getString(R.string.permissionsSystemSettingsButton);
        }
        isDialogOpen = true;
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(R.string.permissionsRequestTitle))
                .setMessage(getMessage(context, requestCode))
                .setPositiveButton(positiveButtonLabel, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        if (requestCode != WRITE_SETTINGS) {
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                intent.setAction(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            }
                        }
                        Uri uri = Uri.fromParts("package", context.getApplicationContext().getPackageName(), null);
                        intent.setData(uri);
                        context.startActivity(intent);
                        isDialogOpen = false;
                    }
                })
                .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialogInterface, int i) {
                        isDialogOpen = false;
                    }
                }).create().show();
    }

    /**
     * Internal method to display a retry dialog when permissions request denied
     *
     * @param context the requesting Activity's context
     * @param requestCode the permissions request code
     */
    private void showRetryDialog(final Context context, @RequestCodes final int requestCode) {
        isDialogOpen = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getString(R.string.permissionsRetryTitle))
                    .setMessage(getMessage(context, requestCode))
                    .setPositiveButton(context.getResources().getString(R.string.permissionsRetryButton), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            isDialogOpen = false;
                            requestNecessaryPermissions((Activity) context, requestCode);
                        }
                    })
                    .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            isDialogOpen = false;
                        }
                    }).create().show();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getString(R.string.permissionsRetryTitle))
                    .setMessage(getMessage(context, requestCode))
                    .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            isDialogOpen = false;
                        }
                    }).create().show();
        }
    }

    /**
     * Determines if requested permissions have been granted
     *
     * @param context the requesting Activity's context
     * @param requestCode the permissions request code
     * @return true if permissions granted; false if not
     */
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("ObsoleteSdkInt")
    public boolean isPermissionGranted(final Context context, @RequestCodes final int requestCode) {
        //sdk 15 doesn't support some of the codes below, always return success
        if (Build.VERSION.SDK_INT < 16) {
            return true;
        }
        // Determine which permissions to check based on request code
        // All possible request codes should be considered
        switch (requestCode) {
//            case READ_CONNECTION_LIST:
//            case CLEAR_CONNECTION_LIST:
//            case STORE_CONNECTION_LIST:
//            case READ_PREFERENCES:
//            case STORE_PREFERENCES:
//            case STORE_FUNCTION_SETTINGS:
//            case STORE_LOG_FILES:
//            case READ_FUNCTION_SETTINGS:
//            case STORE_SERVER_AUTO_PREFERENCES:
//            case READ_SERVER_AUTO_PREFERENCES:
//                return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
//                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//            case READ_LEGACY_FILES:
            case READ_IMAGES:
                return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            case READ_PHONE_STATE:
            case CONNECT_TO_SERVER:
                return  ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
            case ACCESS_FINE_LOCATION :
                return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED;
            case WRITE_SETTINGS:
                boolean result;
                if (Build.VERSION.SDK_INT < 23) {
                    result = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
                } else {
                    result = Settings.System.canWrite(context);
                }
                return result;
            case VIBRATE:
                return ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED;

//<!-- needed for API 33 -->
            case READ_MEDIA_IMAGES:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
                }
//<!-- needed for API 34 -->
            case READ_MEDIA_VISUAL_USER_SELECTED:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED;
                }
//            case NEARBY_WIFI_DEVICES :
//                return ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES ) == PackageManager.PERMISSION_GRANTED;
            case POST_NOTIFICATIONS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS ) == PackageManager.PERMISSION_GRANTED;
                }
//<!-- needed for API 33 -->

            case ACCESS_COARSE_LOCATION :
                return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED;
            case ACCESS_WIFI_STATE :
                return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE ) == PackageManager.PERMISSION_GRANTED;
            case INTERNET :
                return ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET ) == PackageManager.PERMISSION_GRANTED;

            default:
                return false;
        }
    }

    /**
     * Internal method to determine if to show the permission rationale message
     *
     * @param activity the requesting Activity
     * @param requestCode the permissions request code
     * @return true if rationale to be shown; false if not
     */
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean showPermissionRationale(final Activity activity, @RequestCodes final int requestCode) {
        // Determine which permission rationales to check based on request code
        // All possible request codes should be considered
        switch (requestCode) {
//            case READ_CONNECTION_LIST:
//            case CLEAR_CONNECTION_LIST:
//            case STORE_CONNECTION_LIST:
//            case READ_PREFERENCES:
//            case STORE_PREFERENCES:
//            case STORE_FUNCTION_SETTINGS:
//            case STORE_LOG_FILES:
//            case READ_FUNCTION_SETTINGS:
//            case STORE_SERVER_AUTO_PREFERENCES:
//            case READ_SERVER_AUTO_PREFERENCES:
//                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE) &&
//                        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//            case READ_LEGACY_FILES:
            case READ_IMAGES:
                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            case READ_PHONE_STATE:
            case CONNECT_TO_SERVER:
                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE);
            case ACCESS_FINE_LOCATION:
                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION);
            case WRITE_SETTINGS:
                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_SETTINGS);

            case READ_MEDIA_IMAGES:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_MEDIA_IMAGES);
                }
            case READ_MEDIA_VISUAL_USER_SELECTED:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
                }
//            case NEARBY_WIFI_DEVICES:
//                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.NEARBY_WIFI_DEVICES);
            case POST_NOTIFICATIONS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
                }

            case ACCESS_COARSE_LOCATION:
                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
            case ACCESS_WIFI_STATE:
                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_WIFI_STATE);
            case INTERNET:
                return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.INTERNET);

            default:
                return false;
        }
    }

    /**
     * Callback interface to be implemented by any calling Activity
     */
    public interface PermissionsHelperGrantedCallback {
        void navigateToHandler(@RequestCodes int requestCode); //, @ResultCode int resultCode);
    }
}
