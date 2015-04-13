package com.revyuk.krok_helper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by Notebook on 13.03.2015.
 */
public class SameerDB extends SQLiteOpenHelper {
     static final int DB_VERSION = 4;
     static final String DB_NAME = "sammer";
     static final String TABLE_NAME = "medical";

     static final String TABLE_COLUMN_ID = "_id";
     static final String TABLE_COLUMN_QUESTION = "question";
     static final String TABLE_COLUMN_ANSWER = "answer";
     static final String TABLE_COLUMN_FLCODE = "flcode";

    public SameerDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //String query = String.format("CREATE TABLE '%s' INTEGER PRIMARY KEY");
        StringBuilder query = new StringBuilder("create table ");
        query.append(TABLE_NAME)
                .append(" (")
                .append(TABLE_COLUMN_ID).append(" integer")
                .append(", ").append(TABLE_COLUMN_QUESTION).append(" text")
                .append(", ").append(TABLE_COLUMN_ANSWER).append(" text")
                .append(", ").append(TABLE_COLUMN_FLCODE).append(" text")
                .append(")");
        db.execSQL(query.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_NAME);
        onCreate(db);
    }

    public int getCount() {
        int reslt = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) AS 'cnt' FROM " + TABLE_NAME, null);
        if(cursor.moveToFirst()) {
            reslt  = cursor.getInt(0);
        }
        db.close();
        return reslt;
    }

    public void erase() {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM "+TABLE_NAME);
        db.close();
    }

    public ArrayList<QueryResult> searchFlcode(String flcode, boolean fullSearch) {
        ArrayList<QueryResult> list = new ArrayList<>();
        if(flcode.length() > 0) {
            StringBuilder query = new StringBuilder("select * from ")
                    .append(TABLE_NAME)
                    .append(" where ");
                     if(fullSearch) {
                         query.append(TABLE_COLUMN_QUESTION).append(" like \"%").append(flcode).append("%\"")
                         .append(" or ");
                     }
                    query.append(TABLE_COLUMN_FLCODE).append(" like \"%").append(flcode).append("%\"");
            //Log.d("XXX", "QUERY: " + query.toString());
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(query.toString(), null);
            if(cursor.moveToFirst()) {
                do {
                    QueryResult queryResult = new QueryResult();
                    queryResult.answer = cursor.getString(cursor.getColumnIndex(TABLE_COLUMN_ANSWER));
                    queryResult.question = cursor.getString(cursor.getColumnIndex(TABLE_COLUMN_QUESTION));
                    queryResult.flcode = cursor.getString(cursor.getColumnIndex(TABLE_COLUMN_FLCODE));
                    queryResult.question_expanded = false;
                    list.add(queryResult);
                } while(cursor.moveToNext());
            }
            db.close();
        }
        return list;
    }

}
