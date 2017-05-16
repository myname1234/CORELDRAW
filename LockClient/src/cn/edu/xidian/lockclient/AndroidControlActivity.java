package cn.edu.xidian.lockclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 控制端的Activity
 * @author Administrator
 *
 */

public class AndroidControlActivity extends Activity{

	Button btLock = null;
	Button btUnlock = null;
	Button btConnect = null;
	Button btQueryState = null;
	Button btExit = null;
	Socket serverSocket = null;
	EditText etToUser = null;
	TextView tvState = null;
	TextView tvIsOnline = null;
	BufferedReader server_din = null;
	BufferedWriter server_dout = null;
	String toUser = null;
	int isConnected = 0;
	private long exitTime = 0;
	String userName = null;
	
	@SuppressLint("HandlerLeak")
	public Handler myHandler = new Handler(){
		@SuppressLint("HandlerLeak")
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == 1){    //处理连接被控用户的反馈结果
				Bundle bundle = msg.getData();
				
				if(bundle.getString("connect_result").equals("online")){
					isConnected = 1;
					tvIsOnline.setText("该用户当前在线");
					Toast.makeText(AndroidControlActivity.this, "连接对端成功", 
							Toast.LENGTH_SHORT).show();
				}else if(bundle.getString("connect_result").equals("offline")){
					isConnected = 0;
					tvIsOnline.setText("该用户当前离线");
					Toast.makeText(AndroidControlActivity.this, "连接对端失败，请重试", 
							Toast.LENGTH_SHORT).show();
				} else {
					isConnected = 0;
					Toast.makeText(AndroidControlActivity.this, "服务器关闭，无法查询", 
							Toast.LENGTH_SHORT).show();
				}
			}
			if (msg.what == 2) {
				Bundle bundle = msg.getData();
				if (bundle.getString("state").equals("on")) {
					tvState.setText("该用户当前屏幕开启");
				}
				if (bundle.getString("state").equals("off")) {
					tvState.setText("该用户当前屏幕关闭");
				}
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_android_control);
		
		btLock = (Button) findViewById(R.id.btLock);
		btUnlock = (Button) findViewById(R.id.btUnlock);
		btConnect = (Button) findViewById(R.id.btConnect);
		etToUser = (EditText) findViewById(R.id.etToUser);
		btExit = (Button) findViewById(R.id.btExit);
		tvState = (TextView) findViewById(R.id.tvState);
		tvIsOnline = (TextView) findViewById(R.id.tvIsOnline);
		btQueryState = (Button) findViewById(R.id.btQueryState);
		
		Intent intent = getIntent();
		userName = intent.getStringExtra("userName");
		
		//监听连接按钮点击事件
		btConnect.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				toUser = etToUser.getText().toString();
				new Thread() {
					public void run() {
						try{
							Message msg = new Message();
							msg.what = 1;
							Bundle bundle = new Bundle();
							bundle.clear();

							MySocket ss = MySocket.getSingleInstance();
							serverSocket = ss.getSocket();
							server_dout = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream(),"utf-8"));
							server_din = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(),"utf-8"));
							JSONObject json = new JSONObject();

							json.put("fromUser", userName);
							json.put("password", "null");
							json.put("operation", "connect");
							json.put("userType", "master");
							json.put("toUser", "server");
							json.put("msgContent", toUser);

							server_dout.write(json.toString() + "\n");
							server_dout.flush();

							String result = server_din.readLine();
							bundle.putString("connect_result", result);
							msg.setData(bundle);
							myHandler.sendMessage(msg);
							
						}catch(Exception e){
							Log.e("reConnect_Error", e.toString());
							Message errorMsg = new Message();
							errorMsg.what = 1;
							Bundle bundle = new Bundle();
							bundle.clear();
							bundle.putString("connect_result", "error");
							errorMsg.setData(bundle);
							myHandler.sendMessage(errorMsg);
						} 
					};
				}.start();
			}
		});
		
		btQueryState.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(toUser == null) {
					Toast.makeText(AndroidControlActivity.this, "没有输入被控制用户", 
							Toast.LENGTH_SHORT).show();
				} else if (isConnected == 1) {
					MyQueryThread thread = new MyQueryThread();
					thread.start();
					thread.interrupt();  //线程阻塞时抛出中断信号
				} else {
					Toast.makeText(AndroidControlActivity.this, "该用户未上线", 
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		//监听锁屏按钮点击事件
		btLock.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(toUser == null) {
					Toast.makeText(AndroidControlActivity.this, "没有输入被控制用户", 
							Toast.LENGTH_SHORT).show();
				} else if (isConnected == 1) {
					MyLockThread thread = new MyLockThread();
					thread.start();
					tvState.setText("该用户当前屏幕关闭");
					thread.interrupt();  //线程阻塞时抛出中断信号
				} else {
					Toast.makeText(AndroidControlActivity.this, "该用户未上线", 
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		//监听解锁按钮点击事件
		btUnlock.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(toUser == null) {
					Toast.makeText(AndroidControlActivity.this, "没有输入被控制用户", 
							Toast.LENGTH_SHORT).show();
				} else if (isConnected == 1) {
					MyUnlockThread thread = new MyUnlockThread();
					thread.start();
					tvState.setText("该用户当前屏幕开启");
					thread.interrupt();
				} else {
					Toast.makeText(AndroidControlActivity.this, "该用户未上线", 
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		//退出按钮点击事件
		btExit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MyExitThread thread = new MyExitThread();
				thread.start();
				thread.interrupt();
				finish();
				System.exit(0);
			}
		});
	}
	
	/*
	 * 再按一次手机返回键退出程序
	 */
	@SuppressLint("ShowToast")
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN){
			if((System.currentTimeMillis() - exitTime)>2000){
				Toast.makeText(getApplicationContext(), "再按一次退出程序", 
						Toast.LENGTH_SHORT);
				exitTime = System.currentTimeMillis();
			}else{
				if(serverSocket != null){
					MyExitThread thread = new MyExitThread();
					thread.start();
					thread.interrupt();
				}
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private class MyQueryThread extends Thread {
		@Override
		public void run() {
			try {
				Message msg = new Message();
				msg.what = 2;
				Bundle bundle = new Bundle();
				bundle.clear();

				JSONObject json = new JSONObject();
				json.put("fromUser", userName);
				json.put("password", "null");
				json.put("operation", "queryState");
				json.put("userType", "master");
				json.put("toUser", "server");
				json.put("msgContent", toUser);

				server_dout.write(json.toString() + "\n");
				server_dout.flush();

				String result = server_din.readLine();
				bundle.putString("state", result);
				msg.setData(bundle);
				myHandler.sendMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * 锁屏线程
	 */
	private class MyLockThread extends Thread {
		@Override
		public void run() {
			try {
				JSONObject json = new JSONObject();
				json.put("fromUser", userName);
				json.put("password", "null");
				json.put("operation", "lock");
				json.put("userType", "master");
				json.put("toUser", "server");
				json.put("msgContent", toUser);
				server_dout.write(json.toString() + "\n");
				server_dout.flush();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/*
	 * 解锁线程
	 */
	private class MyUnlockThread extends Thread{
		@Override
		public void run() {
			try {
				JSONObject json = new JSONObject();
				json.put("fromUser", userName);
				json.put("password", "null");
				json.put("operation", "unlock");
				json.put("userType", "master");
				json.put("toUser", "server");
				json.put("msgContent", toUser);
				server_dout.write(json.toString() + "\n");
				server_dout.flush();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	/*
	 * 退出线程
	 */
	private class MyExitThread extends Thread {
		@Override
		public void run() {
			try{
				JSONObject json = new JSONObject();
				json.put("fromUser", userName);
				json.put("password", "null");
				json.put("operation", "exit");
				json.put("userType", "master");
				json.put("toUser", "server");
				json.put("msgContent", "null");
				Log.v("msg", json.toString());
				server_dout.write(json.toString() + "\n");
				server_dout.flush();
			}catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
