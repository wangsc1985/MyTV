package com.wangsc.mytv

class Material{
    var Title:String
    var Logo:String
    var FilePath:String
    var Checked:Boolean
    var FileType:Int
    var FileId:Long
    var UploadedSize:Int
    var TimeStamps:String
    var Time:String
    var FileSize:Long

    init {
        Title=""
        Logo=""
        FilePath = ""
        Checked = false
        FileType=0
        FileId = 1
        UploadedSize = 0
        TimeStamps=""
        Time=""
        FileSize=0
    }
}
