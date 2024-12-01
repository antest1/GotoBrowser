package com.antest1.gotobrowser.ContentProvider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.antest1.gotobrowser.Helpers.KcUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KcaPacketStore extends SQLiteOpenHelper {
    public static final int PACKETSTORE_VERSION = 1;
    private static final String db_name = "goto_packet_db";
    private static final String table_name = "packet_store";
    private static int limit = 10;

    public static String getTableName() {
        return table_name;
    }

    public KcaPacketStore(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, db_name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sb = " CREATE TABLE " + table_name +
                " ( DKEY INTEGER PRIMARY KEY, " +
                " TIMESTAMP INTEGER, " +
                " URL TEXT, " +
                " REQUEST TEXT, " +
                " RESPONSE TEXT ) ";
        db.execSQL(sb);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("drop table if exists " + table_name);
        onCreate(db);
    }

    public void record(String url, String request, String response) {
        SQLiteDatabase db = this.getWritableDatabase();

        // filter out api_token
        String[] request_data = request.split("&");
        List<String> new_request_data = new ArrayList<>();
        for (String s: request_data) {
            String decodedData = null;
            try {
                decodedData = URLDecoder.decode(s, "utf-8");
                if (!decodedData.startsWith("api_token")) {
                    new_request_data.add(s);
                }
            } catch (UnsupportedEncodingException e) {
                KcUtils.reportException(e);
            }
        }
        request = KcUtils.joinStr(new_request_data, "&");

        // insert value to db
        ContentValues values = new ContentValues();
        values.put("URL", url);
        values.put("REQUEST", request);
        values.put("RESPONSE", response);
        values.put("TIMESTAMP", System.currentTimeMillis());
        db.insert(table_name, null, values);

        // remove older rows
        db.delete(table_name, "ROWID NOT IN (SELECT ROWID FROM " + table_name +
                " ORDER BY DKEY DESC LIMIT " + limit + ")", null);
    }

    public Cursor getRecentDataCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT URL, REQUEST, RESPONSE, TIMESTAMP FROM " + KcaPacketStore.getTableName() + " ORDER BY DKEY DESC LIMIT 1", null);
    }
}
