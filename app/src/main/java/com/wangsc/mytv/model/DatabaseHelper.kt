package com.wangsc.mytv.model

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * Created by 阿弥陀佛 on 2015/11/18.
 */
class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context,
    DATABASE_NAME,null,
    VERSION
) {

    companion object {
        private const val VERSION = 1
        private const val DATABASE_NAME = "mp.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建数据库后，对数据库的操作
        try {
            db.execSQL(
                "create table if not exists setting("
                        + "name TEXT PRIMARY KEY,"
                        + "value TEXT,"
                        + "level INT NOT NULL DEFAULT 100)"
            )
        } catch (e: SQLException) {
            Log.e("wangsc", e.message)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 更改数据库版本的操作
        try {
            when (oldVersion) {
            }
        } catch (e: SQLException) {
            Log.e("wangsc", e.message)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // 每次成功打开数据库后首先被执行
    }
}