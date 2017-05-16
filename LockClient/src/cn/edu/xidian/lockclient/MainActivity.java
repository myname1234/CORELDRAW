package cn.edu.xidian.lockclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

/**
 * 打开APP时的主页面
 * @author Administrator
 *
 */

public class MainActivity extends Activity {
	
	Button btLogin = null;
	Button btRegister = null;
	Button btExit = null;
	CheckBox cbRemember = null;
	EditText etUserName = null;
	EditText etPassword = null;
	EditText etServerAddr = null;
	RadioButton rbControl = null;
	RadioButton rbBeControled = null;
	
	Socket s = null;
	Integer controlFlag = 1;
	BufferedWriter dout = null;
	BufferedReader din = null;
	
	//在主UI线程和子线程之间通信，进行下一步操作
	@SuppressLint("HandlerLeak")
	public Handler myHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == 1){                          
				Bundle bundle = msg.getData();
				if(bundle.getString("login_result").equals("success")){  //登录成功则跳转到相应页面
					if(controlFlag == 0){                   
						//跳转到控制页面，将用户名传递过去，在另一个页面利用同一socket通信
						Intent controlIntent = new Intent(MainActivity.this,AndroidControlActivity.class);
						controlIntent.putExtra("userName", etUserName.getText().toString());
						startActivity(controlIntent);
						finish();
					}else{
						//跳转到被控页面
						Intent infoViewIntent = new Intent(MainActivity.this,AndroidBeControledActivity.class);
						infoViewIntent.putExtra("userName", etUserName.getText().toString());
						startActivity(infoViewIntent);
						//启动后台service，负责继续通信
						Toast.makeText(MainActivity.this, "启动后台service成功", 
								Toast.LENGTH_SHORT).show();
						Intent intent = new Intent(MainActivity.this,SocketService.class);
						intent.putExtra("userName", etUserName.getText().toString());
						startService(intent);
						finish();
					}
				}
				else if(bundle.getString("login_result").equals("failed")){
					Toast.makeText(MainActivity.this, "登录失败，请检查用户名和密码", 
							Toast.LENGTH_SHORT).show();
				}
				else if(bundle.getString("login_result").equals("isOnline")){
					Toast.makeText(MainActivity.this, "登录失败，用户已经上线", 
							Toast.LENGTH_SHORT).show();
				}
				else if(bundle.getString("login_result").equals("error")){
					Toast.makeText(MainActivity.this, "连接服务器失败，请检查服务器地址", 
							Toast.LENGTH_SHORT).show();
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					s = null;
				}
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		btLogin = (Button) findViewById(R.id.btLogin);
		btRegister = (Button) findViewById(R.id.btRegister);
		btExit = (Button) findViewById(R.id.btExit);
		etUserName = (EditText) findViewById(R.id.etUserName);
		etPassword = (EditText) findViewById(R.id.etPassword);
		etServerAddr = (EditText) findViewById(R.id.etServerAddr);
		cbRemember = (CheckBox) findViewById(R.id.cbRemember);
		rbControl = (RadioButton) findViewById(R.id.rbControl);
		rbBeControled = (RadioButton) findViewById(R.id.rbBeControled);
		
		checkIfRemember();     //记住用户名、密码和服务器IP
		
		//选择作为控制端还是被控端登录
		rbControl.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					Toast.makeText(MainActivity.this, "选中控制端", 
							Toast.LENGTH_SHORT).show();
					controlFlag = 0;
				}else{
					Toast.makeText(MainActivity.this, "选中被控端", 
							Toast.LENGTH_SHORT).show();
					controlFlag = 1;
				}
			}
			
		});
		
		//响应登录按钮点击事件
		btLogin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				final String userName = etUserName.getText().toString();
				final String password = etPassword.getText().toString();
				final String serverAddr = etServerAddr.getText().toString();
				
				if(userName.trim().equals("")){
					etUserName.setError("用户名不能为空！");
					return;
				}else if(password.trim().equals("")){
					etPassword.setError("密码不能为空！");
					return;
				}else if(serverAddr.trim().equals("")){
					etServerAddr.setError("服务器IP不能为空！");
					return;
				}
				if(!isNetworkConnected(MainActivity.this)){
					Toast.makeText(MainActivity.this, "网络未连接", 
							Toast.LENGTH_SHORT).show();
				}else{           //一切正常，则将信息封装成json数据发往服务器
					new Thread(){
						@Override
						public void run() {
							try{
								Message msg = new Message();
								msg.what = 1;
								Bundle bundle = new Bundle();
								bundle.clear();
								
								if(cbRemember.isChecked()){
									rememberMe(userName, password, serverAddr);
								}
								
								if(s == null){   //使用单例socket实现在多个activity中共用同一个socket
									s = new Socket();
									s.connect(new InetSocketAddress(serverAddr,8888),2*1000);
									MySocket ss = MySocket.getSingleInstance();
									ss.setSocket(s);
								}
								din = new BufferedReader(new InputStreamReader(s.getInputStream(),"utf-8"));
								dout =new  BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"utf-8"));
								String userType = null;
								if (controlFlag == 0) {
									userType = "master";
								} else {
									userType = "slave";
								}
								JSONObject json = new JSONObject();
								json.put("fromUser", userName);
								json.put("password", password);
								json.put("operation", "login_" + userType);
								json.put("userType", userType);
								json.put("toUser", "server");
								json.put("msgContent", "null");
								dout.write(json.toString() + "\n");
								dout.flush();
								
								String result = din.readLine();
								bundle.putString("login_result", result);
								msg.setData(bundle);
								myHandler.sendMessage(msg);
							}catch(Exception e){
								Log.e("reConnect_Error", e.toString());
								Message errorMsg = new Message();
								errorMsg.what = 1;
								Bundle bundle = new Bundle();
								bundle.clear();
								bundle.putString("login_result", "error");
								errorMsg.setData(bundle);
								myHandler.sendMessage(errorMsg);
							}
						}
					}.start();
				}
			}
		});
		
		//响应注册按钮点击事件
		btRegister.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//直接跳转到注册界面
				Intent registerIntent = new Intent(MainActivity.this,RegisterActivity.class);
				startActivity(registerIntent);
				finish();
			}
		});
		
		//响应退出按钮点击事件
		btExit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
				System.exit(0);
			}
		});
	}
	
	/*
	 * 将存储的用户名、密码和服务器IP显示在输入框中
	 */
	public void checkIfRemember(){
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		String userName = sp.getString("userName", null);
		String password = sp.getString("password", null);
		String serverAddr = sp.getString("serverAddr", null);
		if(userName != null && password != null && serverAddr != null){
			etUserName.setText(userName);
			etPassword.setText(password);
			etServerAddr.setText(serverAddr);
			cbRemember.setChecked(true);
		}
	}

	/*
	 * 检查网络是否连接
	 */
	public boolean isNetworkConnected(Context context){
		if(context != null){
			ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if(networkInfo != null){
				return networkInfo.isAvailable();
			}
		}
		return false;
	}
	
	/*
	 * 存储用户名、密码和服务器IP
	 */
	public void rememberMe(String userName,String password,String serverAddr){
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("userName", userName);
		editor.putString("password", password);
		editor.putString("serverAddr", serverAddr);
		editor.commit();
	}
}
