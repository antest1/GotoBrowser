package com.antest1.gotobrowser.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class VersionDatabase extends SQLiteOpenHelper {
    private static final String db_name = "gotobrowser_db";
    private static final String table_name = "version_table";

    public VersionDatabase(Context context, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, db_name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sb = " CREATE TABLE IF NOT EXISTS " + table_name +
                " ( \"KEY\" TEXT PRIMARY KEY, " +
                " VALUE TEXT ) ";
        db.execSQL(sb);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + table_name);
        onCreate(db);
    }

    public void clearVersionDatabase() {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("delete from " + table_name);
    }

    public static boolean isDefaultValue(String text) {
        return "_none_".equals(text);
    }

    // for kca_userdata
    public String getValue(String key) {
        String value = "_none_";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(table_name, null, "KEY=?", new String[]{key}, null, null, null, null);
        try {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                value = c.getString(c.getColumnIndex("VALUE"));
            }
        } catch (Exception e) {
            KcUtils.reportException(e);
        } finally {
            if (c != null) c.close();
        }
        Log.e("GOTO", "getValue " + key + " " + value);
        return value;
    }

    public void putValue(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("KEY", key);
        values.put("VALUE", value);
        int u = db.update(table_name, values, "KEY=?", new String[]{key});
        if (u == 0) {
            db.insertWithOnConflict(table_name, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public String getDefaultValue() {
        return "_none_";
    }

    public void putDefaultValue(String key) {
        putValue(key, "_none_");
    }

}
