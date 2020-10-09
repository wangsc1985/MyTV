package com.example.win

import java.io.DataInputStream
import java.net.ServerSocket

class MyClass {
    companion object {
        /** 我是main入口函数 **/
        @JvmStatic
        fun main(args: Array<String>) {
            var ss = ServerSocket(8000);
            //不止接受一个客户端
            while (true) {
                var s = ss.accept();//接受一个连接
                var dis = DataInputStream(s.getInputStream());//输入管道
                System.out.println(dis.readInt());
                dis.close();
                s.close()
            }
        }
    }
}