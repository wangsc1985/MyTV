package com.wangsc.buddhatv.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.wangsc.buddhatv.R;

public class _NotificationUtils {

    public static Notification sendNotification(Context context) {
        try {
//            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layoutId);

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel("channel_id", "我的手机", NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true);
                channel.enableVibration(true);

                Notification notification = new Notification.Builder(context, "channel_id").setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
                        .setAutoCancel(false)
//                        .setContent(remoteViews)//在这里设置自定义通知的内容
                        .build();
                return notification;
            } else {
                Notification notification = new NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher)//通知的构建过程基本与默认相同
                        .setAutoCancel(false)
//                        .setContent(remoteViews)//在这里设置自定义通知的内容
                        .build();
                return notification;
            }
        } catch (Resources.NotFoundException e) {
        }
        return null;
    }

    public static void closeNotification(Context context,int notificationId){
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
    }
}
