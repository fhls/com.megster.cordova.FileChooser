package com.megster.cordova;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.content.ClipData;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class FileChooser extends CordovaPlugin {

    private static final String TAG = "FileChooser";
    private static final String ACTION_PICK_FILE = "pick_file";
	private static final String ACTION_PICK_FILES = "pick_files";
    private static final int PICK_FILE_REQUEST = 1;
	private static final int PICK_FILES_REQUEST = 2;
    CallbackContext callback;

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        if (action.equals(ACTION_PICK_FILE)) {
            chooseFile(callbackContext);
            return true;
        } else if (action.equals(ACTION_PICK_FILES)) {
			chooseFiles(callbackContext);
            return true;
		}

        return false;
    }

    public void chooseFile(CallbackContext callbackContext) {

        // type and title should be configurable

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        //intent.putExtra(Intent.EXTRA_LOCAL_ONLY, false);

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

	public void chooseFiles(CallbackContext callbackContext) {

        // type and title should be configurable

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        //intent.putExtra(Intent.EXTRA_LOCAL_ONLY, false);

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, PICK_FILES_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE_REQUEST && callback != null) {
			// When pick one file
            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();

                if (uri != null) {

                    Log.w(TAG, uri.toString());

                    try {
                        Context context = cordova.getActivity();
                        String mimeType = context.getContentResolver().getType(uri);
                        String filename = "";

                        if (mimeType == null) {
                            String path = getPath(context, uri);
                            if (path == null) {
                                filename = getName(uri.toString());
                            } else {
                                File file = new File(path);
                                filename = file.getName();
                            }
                        } else {
                            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
                            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                            returnCursor.moveToFirst();
                            filename = returnCursor.getString(nameIndex);
                            String size = Long.toString(returnCursor.getLong(sizeIndex));
                        }

                        //File fileSave = context.getExternalFilesDir(null);
                        String sourcePath = context.getExternalFilesDir(null).toString();

                        try {

                            File savedFile = new File(sourcePath + "/" + filename);

                            copyFileStream(savedFile, uri, context);

                            callback.success(savedFile.toURI().toString());

                        } catch (Exception e) {
                            Log.w(TAG, e.getMessage());
                        }
                    } catch (Exception e) {
                        Log.w(TAG, e.getMessage());
                    }
                }
            } else {
                callback.error("File uri was null");
            }
        } else if (requestCode == PICK_FILES_REQUEST && callback != null) {
			// When pick multiple files
            if (resultCode == Activity.RESULT_OK) {

                ClipData clipData = data.getClipData(); 
				
				if (clipData != null) {  

					final int clipDataCount = clipData.getItemCount();

					if(clipDataCount>0) {
                        JSONArray savedUris = new JSONArray();

                        for (int i = 0; i < clipDataCount; i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();

                            if (uri != null) {

                                Log.w(TAG, uri.toString());

                                try {
                                    Context context = cordova.getActivity();
                                    String mimeType = context.getContentResolver().getType(uri);
                                    String filename = "";

                                    if (mimeType == null) {
                                        String path = getPath(context, uri);
                                        if (path == null) {
                                            filename = getName(uri.toString());
                                        } else {
                                            File file = new File(path);
                                            filename = file.getName();
                                        }
                                    } else {
                                        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
                                        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                                        returnCursor.moveToFirst();
                                        filename = returnCursor.getString(nameIndex);
                                        String size = Long.toString(returnCursor.getLong(sizeIndex));
                                    }

                                    //File fileSave = context.getExternalFilesDir(null);
                                    String sourcePath = context.getExternalFilesDir(null).toString();

                                    try {

                                        File savedFile = new File(sourcePath + "/" + filename);

                                        copyFileStream(savedFile, uri, context);

                                        savedUris.put(savedFile.toURI().toString());

                                    } catch (Exception e) {
                                        Log.w(TAG, e.getMessage());
                                    }

                                } catch (Exception e) {
                                    Log.w(TAG, e.getMessage());
                                }

                            }
                        }

                        String json = savedUris.toString();

                        callback.success(json);
                    } else {
                        callback.error("User didn't pick any files");
                    }
				}
            } else {
                callback.error("File uri was null");
            }
		} else if (resultCode == Activity.RESULT_CANCELED) {

            // TODO NO_RESULT or error callback?
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            callback.sendPluginResult(pluginResult);

        } else {

            callback.error(resultCode);
        }
    }

    public static int indexOfLastSeparator(String filename) {
        if (filename == null) {
            return -1;
        }
        int lastUnixPos = filename.lastIndexOf('/');
        int lastWindowsPos = filename.lastIndexOf('\\');
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = indexOfLastSeparator(filename);
        return filename.substring(index + 1);
    }

    private void copyFileStream(File dest, Uri uri, Context context) throws IOException {
        java.io.InputStream is = null;
        java.io.OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            os = new java.io.FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            is.close();
            os.close();
        }
    }

    public static String getPath(Context context, Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
