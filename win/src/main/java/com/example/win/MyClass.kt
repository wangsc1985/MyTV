package com.example.win

import java.io.DataInputStream
import java.io.File
import java.io.FileFilter
import java.net.ServerSocket
import java.text.DecimalFormat

class MyClass {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var dir = File("https://js1.amtb.cn/media/himp4/02/02-037/02-037-0.mp4")
            println(dir.name)
            println(dir.path)
            println(dir.absolutePath)
        }
    }
}