//package com.wangsc.mytv
//
//import android.os.Parcel
//import android.os.Parcelable
//
//class Material : Parcelable {
//    private var mLogo:String
//    private var title:String
//    private var time:String
//    private var filePath:String
//    private var isChecked:Boolean
//    private var fileSize:Long
//    private var fileId:Long
//    private var uploadedSize:Long
//    private var fileType:Int
//    private var uploaded:Boolean
//    private var progress:Int //上传进度
//    private var timeStamps:String //时间戳
//    private var flag:Int //上传标志 0-正常 1--网络错误 2--超时(除了0以为均为上传失败标识)
//
//
//    @JvmStatic
//    fun  getCREATOR(): Parcelable.Creator<Material> {
//        return CREATOR;
//    }
//
//    override fun toString(): String {
//        return "MaterialBean{" +
//                "mLogo='" + mLogo + '\'' +
//                ", title='" + title + '\'' +
//                ", time='" + time + '\'' +
//                ", filePath='" + filePath + '\'' +
//                ", isChecked=" + isChecked +
//                ", fileSize=" + fileSize +
//                ", fileId=" + fileId +
//                ", uploadedSize=" + uploadedSize +
//                ", fileType=" + fileType +
//                ", uploaded=" + uploaded +
//                ", progress=" + progress +
//                ", timeStamps='" + timeStamps + '\'' +
//                ", flag='" + flag + '\'' +
//                '}';
//    }
//
//    constructor(in:Parcel) {
//        mLogo = in.readString();
//        title = in.readString();
//        time = in.readString();
//        filePath = in.readString();
//        isChecked = in.readByte() != 0;
//        fileSize = in.readLong();
//        fileId = in.readLong();
//        uploadedSize = in.readLong();
//        fileType = in.readInt();
//        uploaded = in.readByte() != 0;
//        progress = in.readInt();
//        timeStamps = in.readString();
//        flag = in.readInt();
//    }
//
//    @JvmField
//    CREATOR = Creator<Material>()
//
////    {
////        @Override
////        public Material createFromParcel(Parcel in) {
////            return new Material(in);
////        }
////
////        @Override
////        public Material[] newArray(int size) {
////            return new Material[size];
////        }
////    }
//
//    override fun describeContents():Int {
//        return 0
//    }
//
//    override fun writeToParcel(dest: Parcel, flags:Int) {
//        dest.writeString(mLogo);
//        dest.writeString(title);
//        dest.writeString(time);
//        dest.writeString(filePath);
//        dest.writeByte((byte) (isChecked ? 1 : 0));
//        dest.writeLong(fileSize);
//        dest.writeLong(fileId);
//        dest.writeLong(uploadedSize);
//        dest.writeInt(fileType);
//        dest.writeByte((byte) (uploaded ? 1 : 0));
//        dest.writeInt(progress);
//        dest.writeString(timeStamps);
//        dest.writeInt(flag);
//    }
//}