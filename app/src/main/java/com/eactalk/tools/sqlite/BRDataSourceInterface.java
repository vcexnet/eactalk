package com.eactalk.tools.sqlite;

import android.database.sqlite.SQLiteDatabase;
public interface BRDataSourceInterface {

    SQLiteDatabase openDatabase();
    void closeDatabase();
}
