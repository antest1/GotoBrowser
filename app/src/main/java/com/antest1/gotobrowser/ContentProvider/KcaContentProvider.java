package com.antest1.gotobrowser.ContentProvider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import static com.antest1.gotobrowser.ContentProvider.KcaPacketStore.PACKETSTORE_VERSION;


public class KcaContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.antest1.gotobrowser.contentprovider";
    public static final String PATH  = "/request";
    public static final Uri CONTENT_URI = Uri.parse("content://".concat(AUTHORITY).concat(PATH));
    public static final String BROADCAST_ACTION = "com.antest1.gotobrowser.broadcast";

    private KcaPacketStore packetTable;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, PATH, 1);
    }

    @Override
    public boolean onCreate() {
        packetTable = new KcaPacketStore(getContext(), null, PACKETSTORE_VERSION);
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projections, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        switch (sUriMatcher.match(uri)){
            case 1:
                cursor = packetTable.getRecentDataCursor();
                break;
            default:
                break;
        }
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // Do Nothing
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        // Do Nothing
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        // Do Nothing
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        // Do Nothing
        return 0;
    }
}