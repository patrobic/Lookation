package com.example.patrickrobichaud.lookation;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class DatabaseSQL extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Location_Logs";
    private static final String KEY_LATITUDE = "Latitude";
    private static final String KEY_LONGITUDE = "Longitude";
    private static final String KEY_DATE = "Date";
    private static final String CREATE_TABLE = " (" + KEY_DATE + " INTEGER UNIQUE," + KEY_LATITUDE + " DOUBLE NOT NULL," + KEY_LONGITUDE + " DOUBLE NOT NULL" + ");";

    public DatabaseSQL(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    // create new index table on first run, if it does not already exist
    @Override public void onCreate(SQLiteDatabase sqLiteDatabase) { sqLiteDatabase.execSQL("CREATE TABLE IndexTable (ID INTEGER UNIQUE, Name TEXT)"); }

    @Override public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) { }

    // create new log table and corresponding pointer in indextable, used when initiating logging
    public int CreateLogTable(String tentativelogname) {
        String logname = CheckNameExists(tentativelogname, 0); // check if log name is unique
        int tablenum = getLogCount(); // get next available number for log ID
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE " + logname + CREATE_TABLE); // create new table with unique name
        ContentValues newtableindex = new ContentValues(); // build content for new indextable entry
        newtableindex.put("ID", tablenum);
        newtableindex.put("Name", logname);
        db.insert("IndexTable", null, newtableindex); // insert content into indextable
        // db.close();
        return tablenum;
    }

    // RECURSIVE - verify if user input log name is unique, and if not append number (A -> A_1, A_2, ..., A_n)
    public String CheckNameExists(String name, int number) {
        List<String> tablelist = getTableList(); // acquire complete list of names
        String testname = name; // create temporary local string
        if (number > 0) testname = name + "_" + number; // if not 0th iteration, append number in "string (#)" format
        for(int i = 0; i < tablelist.size(); i++) // compare temporary string with all names in list
            if (testname.toLowerCase().equals(tablelist.get(i).toLowerCase())) // convert strings to lowercase for comparison, to avoid duplicates of varying case (possible BUG)
                testname = CheckNameExists(name, ++number); // recurse with next number
        return testname; // when name is unique, return name with appended number
    }

    // Create a new log entry and store it into specified table, used to process location tracking data
    public void CreateEntry(LogEntry entry, int tablenum) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues(); // create new database row content
        values.put(KEY_LATITUDE, entry.getLatitude()); // build row by inserting latitude, longitude and date
        values.put(KEY_LONGITUDE, entry.getLongitude());
        values.put(KEY_DATE, entry.getDate());

        db.insert(getTableName(tablenum), null, values); // insert row content into database
        // db.close();
    }

    // get a compact list of all entries for a given log, in LogEntry Class format, used to display data
    public List<LogEntry> getEntryList(int tablenum) {
        List<LogEntry> EntryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + getTableName(tablenum), null); // select all entries is desired log
        if(cursor.moveToFirst()) {
            do {
                LogEntry reconstructed = new LogEntry(cursor.getDouble(1), cursor.getDouble(2), cursor.getString(0)); // reconstruct LogEntry from root database members
                EntryList.add(reconstructed); // append LogEntry to the entry list
            } while (cursor.moveToNext());
        }
        cursor.close();
        // db.close();
        return EntryList;
    }

    // TODO implement function that returns the output of getEntryList for each Log Set stored into a 3D List<List<LogEntry>>. Use output of getTableList as iterative element for string of db.rawQuery(Table_#??), and its size as loop range.
    public List<List<LogEntry>> getLogList() {
        List<List<LogEntry>> LogList = new ArrayList<>();
        for(int i = 0; i < getLogCount(); i++) LogList.add(getEntryList(i)); // call getEntryList for all logs and build an array from its output
        return LogList;
    }


    // acquire list of table names, used to populate Spinner and check if log name already exists
    public List<String> getTableList() {
        List<String> TableList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM IndexTable", null); // select all pointers in indextable
        if(cursor.moveToFirst()) {
            do TableList.add(cursor.getString(1)); // insert name of pointer to list
                while (cursor.moveToNext());
        }
        cursor.close();
        // db.close();
        return TableList;
    }

    // remove specified log from table, shifting all subsequent logs by 1
    public void deleteLog(int tablenum) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE " + getTableName(tablenum)); // remove table from database
        db.execSQL("DELETE FROM IndexTable WHERE ID = " + tablenum); // remove entry pointing to desired table from master indextable
        for(int i = tablenum; i <= getLogCount(); i++) { // cycle through all indexes AFTER deleted index
            ContentValues data = new ContentValues();
            data.put("ID", i); // decrement ID by one to "pop bubble"
            db.update("IndexTable", data, "ID = " + (i+1), null); // overwrite index ID with decremented ID
        }
    }

    // count the total number of logs (for listing or updating list after delete)
    public int getLogCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT  * FROM IndexTable", null); // select all entries in indextable
        int cnt = cursor.getCount(); // count all elements in cursor
        cursor.close();
        return cnt;
    }

    // get name of table at given index ID
    public String getTableName(int tablenum) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor index = db.rawQuery("SELECT * FROM IndexTable", null); // select all entries in indextable
        index.moveToPosition(tablenum); // move to entry corresponding to desire log name
        String name = index.getString(1); // get log name (second column)
        //// db.close();
        return name;
    }

    // function that accepts a button and its activity, and reenables the button in a thread after specified delay
    public void delayButtonEnable(final Button button, final Activity activity) {
        Thread delay;
        (delay = new Thread() { public void run() {
            android.os.SystemClock.sleep(300);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    button.setEnabled(true);
                }
            });
        } }).start();
    }
}