package com.example.win

import java.io.DataInputStream
import java.io.File
import java.io.FileFilter
import java.net.ServerSocket

class MyClass {
    companion object {
        /** 我是main入口函数 **/
        private  var a = 0
        @JvmStatic
        fun main(args: Array<String>) {

            val dir = File("d:\\a")
            var list = dir.listFiles({ file -> file.extension == "wmv" || file.extension == "mp" })
//            var list = dir.listFiles()
            println(list.toList())
            list.forEach {
                println(it.name)
            }
        }
    }
}