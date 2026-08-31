package com.neko.finance;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLConnection;

public class AttachmentProvider extends ContentProvider {
    public static Uri uriForFile(Context context, String movementId, String attachmentId) {
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".attachments")
                .appendPath(movementId)
                .appendPath(attachmentId)
                .build();
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        if (getContext() == null || uri.getPathSegments().size() != 2) throw new FileNotFoundException();
        String movementId = uri.getPathSegments().get(0);
        String attachmentId = uri.getPathSegments().get(1);
        if (!movementId.matches("[A-Za-z0-9_.-]{1,160}") || !attachmentId.matches("[A-Za-z0-9._-]{1,220}")) throw new FileNotFoundException();
        File root = new File(getContext().getFilesDir(), "attachments");
        File file = new File(new File(root, movementId), attachmentId);
        try {
            if (!file.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator) || !file.isFile()) throw new FileNotFoundException();
        } catch (Exception e) { throw new FileNotFoundException(); }
        return file;
    }

    private String displayName(File file) {
        String name = file.getName();
        int separator = name.indexOf("__");
        return separator >= 0 && separator + 2 < name.length() ? name.substring(separator + 2) : name;
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public String getType(Uri uri) {
        try {
            String name = resolve(uri).getName().toLowerCase();
            if (name.endsWith(".pdf")) return "application/pdf";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".webp")) return "image/webp";
            if (name.endsWith(".gif")) return "image/gif";
            if (name.endsWith(".heic")) return "image/heic";
            if (name.endsWith(".heif")) return "image/heif";
            String type = URLConnection.guessContentTypeFromName(name);
            return type == null ? "application/octet-stream" : type;
        } catch (Exception e) { return "application/octet-stream"; }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = resolve(uri);
            MatrixCursor cursor = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, 1);
            cursor.addRow(new Object[]{displayName(file), file.length()});
            return cursor;
        } catch (Exception e) { return null; }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
