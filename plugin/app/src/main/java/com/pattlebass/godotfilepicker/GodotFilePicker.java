package com.pattlebass.godotfilepicker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class GodotFilePicker extends org.godotengine.godot.plugin.GodotPlugin {
    private static final String TAG = "godot";
    private Activity activity;

    private static final int OPEN_FILE = 0;
    private static final int OPEN_DIRECTORY = 1;

    public GodotFilePicker(Godot godot) {
        super(godot);
        activity = godot.getActivity();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "AndroidFilePicker";
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();

        signals.add(new SignalInfo("file_picked", String.class, String.class));
        signals.add(new SignalInfo("directory_picked", String.class));
        return signals;
    }

    @UsedByGodot
    public void openFilePicker(String initialPath, String type) {
        //Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType(type.isEmpty() ? "*/*" : type);
        //chooseFile = Intent.createChooser(chooseFile, "Choose a project");
        chooseFile.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(new File(Environment.DIRECTORY_DOWNLOADS)));
        activity.startActivityForResult(chooseFile, OPEN_FILE);
    }

    @UsedByGodot
    public void openDirectoryPicker(String initialPath) {
        Intent chooseDirectory = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if(initialPath != null && !initialPath.isEmpty() && Uri.parse(initialPath) != null) {
            chooseDirectory.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialPath);
        }
        activity.startActivityForResult(chooseDirectory, OPEN_DIRECTORY);
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onMainActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(resultCode != Activity.RESULT_OK || resultData == null) {
            return;
        }
        if (requestCode == OPEN_FILE) {
            Uri uri = resultData.getData();
            try {
                Log.d(TAG, "Picked file with URI: " + uri.getPath());
                emitSignal("file_picked", getFile(activity, uri).getPath(),
                        activity.getContentResolver().getType(uri));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == OPEN_DIRECTORY) {
            Uri uri = resultData.getData();
            try {
                Log.d(TAG, "Picked folder with URI: " + uri.getPath());

                // Take persistable URI permission
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);

                emitSignal("directory_picked", uri.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // From https://stackoverflow.com/questions/65447194/how-to-convert-uri-to-file-android-10
    public static File getFile(Context context, Uri uri) throws IOException {
        String directoryName = context.getFilesDir().getPath() + File.separatorChar + "_temp";
        File directory = new File(directoryName);
        if (! directory.exists()){
            directory.mkdir();
        }
        File destinationFilename = new File(directoryName + File.separatorChar + queryName(context, uri));
        try (InputStream ins = context.getContentResolver().openInputStream(uri)) {
            createFileFromStream(ins, destinationFilename);
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
        return destinationFilename;
    }

    public static void createFileFromStream(InputStream ins, File destination) {
        try (OutputStream os = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = ins.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static String queryName(Context context, Uri uri) {
        Cursor returnCursor =
                context.getContentResolver().query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    public void testPermissions(String uri) {
        // TODO
    }

    public void readFile(String uri) {
        // TODO
    }

    public void writeFile(String uri) {
        // TODO
    }
}
