package cn.edu.xidian.lockclient;

import android.app.admin.DeviceAdminReceiver; 
import android.content.Context; 
import android.content.Intent; 
/*
 * 功能：启动锁屏功能广播接收器，即使锁屏功能能被本APP调用
 */
public class MyReceiver extends DeviceAdminReceiver{ 
   
   
    @Override 
    public void onReceive(Context context, Intent intent) { 
        super.onReceive(context, intent); 
    } 
   
    @Override 
    public void onEnabled(Context context, Intent intent) { 
        
        super.onEnabled(context, intent); 
    } 
   
    @Override 
    public void onDisabled(Context context, Intent intent) { 
        super.onDisabled(context, intent); 
    } 
   
   
} 
