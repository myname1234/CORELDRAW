package cn.edu.xidian.lockclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.KeyguardManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

/**
 * 负责接收控制指令，进行锁屏解锁操作
 * @author Administrator
 *
 */

public class SocketService extends Service{

	Socket s;
	BufferedReader din;
	BufferedWriter dout;
	Handler myHandler;
	Handler warn_active_deviceManagerHandler;
	boolean flag = true;
	IntentFilter Destroy;
	DestroyServiceReceiver destroyServiceReceiver;
	IntentFilter HisInfo;
	IntentFilter onIntentFilter;
	IntentFilter offIntentFilter;
	MyOnBroadcastReceiver onReceiver;
	MyOffBroadcastReceiver offReceiver;
	String userName = null;
	
	private DevicePolicyManager policyManger;
	private ComponentName componentName;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	//关闭该服务
	@Override
	public void onDestroy() {
		try{
			//解除所有广播绑定
			unregisterReceiver(onReceiver);
			unregisterReceiver(offReceiver);
			unregisterReceiver(destroyServiceReceiver);
			dout.close();
			din.close();
			s.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try{
			MySocket ss = MySocket.getSingleInstance();
			s = ss.getSocket();
			dout = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"utf-8"));
			din = new BufferedReader(new InputStreamReader(s.getInputStream(),"utf-8"));
			
			userName = intent.getStringExtra("userName");
			
			//注册关闭service的广播接受者
			Destroy = new IntentFilter("stopService");
			destroyServiceReceiver = new DestroyServiceReceiver();
			registerReceiver(destroyServiceReceiver, Destroy);
			
			//注册监控屏幕状态的广播接受者
			onIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			offIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
			onReceiver = new MyOnBroadcastReceiver();
			offReceiver = new MyOffBroadcastReceiver();
			registerReceiver(onReceiver, onIntentFilter);
			registerReceiver(offReceiver, offIntentFilter);
		}catch (IOException e){
			e.printStackTrace();
		}
		
		//创建线程接收服务器指令
		new Thread(){
			@Override
			public void run(){
				String result = new String();
				
				while(!s.isClosed()){
					try {
						result = din.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
					if(result == null){
						Intent intent = new Intent("SocketClose");
						sendBroadcast(intent);
						break;
					}
					if(result.equals("lock")){
						MyLock();
					}else if(result.equals("unlock")){
						MyCallScreenOn(SocketService.this);
					}else if(result.equals("#END#")){
						Intent intent = new Intent("SocketClose");
						sendBroadcast(intent);
						flag = false;
						
						myHandler = new Handler(Looper.getMainLooper());
						myHandler.post(new Runnable(){
							@Override
							public void run(){
								Toast.makeText(getApplicationContext(), "服务器关闭，退出控制", 
										Toast.LENGTH_SHORT).show();
							}
						});
						onDestroy();
					}
				}
			}
		}.start();
		return super.onStartCommand(intent, flags, startId);
	}
	
	//解锁
	@SuppressWarnings("deprecation")
	private synchronized void MyCallScreenOn(Context context){
		KeyguardManager km= (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);      
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");      
        //解锁      
        kl.disableKeyguard();      
        //获取电源管理器对象      
        PowerManager pm=(PowerManager) context.getSystemService(Context.POWER_SERVICE);      
        //获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag      
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK,"bright");      
        //点亮屏幕      
        wl.acquire();      
        //释放      
        wl.release();      
	}
	
	//锁屏
	private synchronized void MyLock(){
		policyManger = (DevicePolicyManager) getSystemService(RegisterActivity.DEVICE_POLICY_SERVICE);
		componentName = new ComponentName(this,MyReceiver.class);//锁屏后激活广播事件
		
		if(policyManger.isAdminActive(componentName)){
			policyManger.lockNow();
		}else{
			warn_active_deviceManagerHandler = new Handler(Looper.getMainLooper());
			warn_active_deviceManagerHandler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "您还没有开启设备管理器", 
							Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
	
	//屏幕亮广播接收者
	public class MyOnBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				JSONObject json = new JSONObject();
				json.put("fromUser", userName);
				json.put("password", "null");
				json.put("operation", "on");
				json.put("userType", "slave");
				json.put("toUser", "server");
				json.put("msgContent", "null");
				dout.write(json.toString() + "\n");
				dout.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//屏幕关闭广播接收者
	public class MyOffBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				JSONObject json = new JSONObject();
				json.put("fromUser", userName);
				json.put("password", "null");
				json.put("operation", "off");
				json.put("userType", "slave");
				json.put("toUser", "server");
				json.put("msgContent", "null");
				dout.write(json.toString() + "\n");
				dout.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	//结束service广播接受者
	public class DestroyServiceReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			stopSelf();
		}
		
	}
}
