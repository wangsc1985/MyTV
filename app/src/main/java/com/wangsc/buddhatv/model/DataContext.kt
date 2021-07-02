package com.wangsc.buddhatv.model

import android.content.ContentValues
import android.content.Context
import java.util.*

/**
 * Created by 阿弥陀佛 on 2015/11/18.
 */
class DataContext(context: Context) {
    private val dbHelper: DatabaseHelper

    init {
        dbHelper = DatabaseHelper(context)
    }

    private fun addMediaPosition(mediaPosition: MediaPosition) {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        //使用insert方法向表中插入数据
        val values = ContentValues()
        values.put("dirName", mediaPosition.dirName)
        values.put("filePath", mediaPosition.filePath)
        values.put("position", mediaPosition.position)
        //调用方法插入数据
        db.insert("MediaPosition", "dirName", values)
        //关闭SQLiteDatabase对象
        db.close()
    }

    fun editMediaPosition(dirName: String,filePath:String,position:Int) {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        //使用update方法更新表中的数据
        val values = ContentValues()
        values.put("filePath", filePath)
        values.put("position", position)
        if (db.update("MediaPosition", values, "dirName=?", arrayOf(dirName)) == 0) {
            addMediaPosition(MediaPosition(dirName,filePath,position))
        }
        db.close()
    }

    fun getMediaPosition(dirName: String): MediaPosition? {
        //获取数据库对象
        val db = dbHelper.readableDatabase
        //查询获得游标
        val cursor = db.query("MediaPosition",null, "dirName=?", arrayOf(dirName), null, null, null)
        //判断游标是否为空
        while (cursor.moveToNext()) {
            val result = MediaPosition( dirName.toString(),
                cursor.getString(1),
                cursor.getInt(2) )
            cursor.close()
            db.close()
            return result
        }
        return null
    }

    //region Setting
    fun getSetting(name: Any): Setting? {

        //获取数据库对象
        val db = dbHelper.readableDatabase
        //查询获得游标
        val cursor = db.query("setting",null, "name=?", arrayOf(name.toString()), null, null, null)
        //判断游标是否为空
        while (cursor.moveToNext()) {
            val setting = Setting( name.toString(),
                    cursor.getString(1),
                    cursor.getInt(2) )
            cursor.close()
            db.close()
            return setting
        }
        return null
    }

    fun getSetting(name: Any, defaultValue: Any): Setting {
        var setting = getSetting(name)
        if (setting == null) {
            addSetting(name, defaultValue)
            setting = Setting(
                name.toString(),
                defaultValue.toString(),
                100
            )
            return setting
        }
        return setting
    }

    /**
     * 修改制定key配置，如果不存在则创建。
     *
     * @param name
     * @param value
     */
    fun editSetting(name: Any, value: Any) {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        //使用update方法更新表中的数据
        val values = ContentValues()
        values.put("value", value.toString())
        if (db.update("setting", values, "name=?", arrayOf(name.toString())) == 0) {
            addSetting(name, value.toString())
        }
        db.close()
    }

    fun editSettingLevel(name: Any, level: Int) {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        //使用update方法更新表中的数据
        val values = ContentValues()
        values.put("level", level.toString() + "")
        db.update("setting", values, "name=?", arrayOf(name.toString()))
        db.close()
    }

    fun deleteSetting(name: Any) {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        db.delete("setting", "name=?", arrayOf(name.toString()))
        //        String sql = "DELETE FROM setting WHERE userId="+userId.toString()+" AND name="+name;
//        addLog(new Log(sql,userId),db);
        //关闭SQLiteDatabase对象
        db.close()
    }

    fun deleteSetting(name: String) {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        db.delete("setting", "name=?", arrayOf(name))
        //        String sql = "DELETE FROM setting WHERE userId="+userId.toString()+" AND name="+name;
//        addLog(new Log(sql,userId),db);
        //关闭SQLiteDatabase对象
        db.close()
    }

    fun addSetting(name: Any, value: Any) {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        //使用insert方法向表中插入数据
        val values = ContentValues()
        values.put("name", name.toString())
        values.put("value", value.toString())
        //调用方法插入数据
        db.insert("setting", "name", values)
        //关闭SQLiteDatabase对象
        db.close()
    }

    //获取数据库对象
    val settings: List<Setting>
        get() {
            val result: MutableList<Setting> =
                ArrayList()
            //获取数据库对象
            val db = dbHelper.readableDatabase
            //查询获得游标
            val cursor =
                db.query("setting", null, null, null, null, null, "level,name")
            //判断游标是否为空
            while (cursor.moveToNext()) {
                val setting =
                    Setting(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getInt(2)
                    )
                result.add(setting)
            }
            cursor.close()
            db.close()
            return result
        }

    fun clearSetting() {
        //获取数据库对象
        val db = dbHelper.writableDatabase
        db.delete("setting", null, null)
        //        String sql = "DELETE FROM setting WHERE userId="+userId.toString()+" AND key="+key;
//        addLog(new Log(sql,userId),db);
        //关闭SQLiteDatabase对象
        db.close()
    } //endregion

}