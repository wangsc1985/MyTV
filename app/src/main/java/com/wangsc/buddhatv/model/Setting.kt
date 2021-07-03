package com.wangsc.buddhatv.model

/**
 * Created by 阿弥陀佛 on 2015/6/30.
 */
class Setting(var name: String, var string: String, var level: Int) {

    fun setValue(value: String) {
        string = value
    }

    val boolean: Boolean
        get() = java.lang.Boolean.parseBoolean(string)

    val int: Int
        get() = string.toInt()

    val long: Long
        get() = string.toLong()

    val dateTime: DateTime
        get() = DateTime(long)

    val float: Float
        get() = string.toFloat()

    val double: Double
        get() = string.toDouble()

    enum class KEYS  //endregion
    {
        media_title,operate_name
    }

}