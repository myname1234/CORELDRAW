package cn.edu.xidian.lockclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 注册界面，用户注册成功之后会跳转得到被控制界面
 * @author Administrator
 *
 */

public class RegisterActivity extends Activity {
	Button btRegister = null;//注册按钮
	Button btExit = null;//退出按钮
	EditText etUserName = null;
	EditText etPassword = null;
	EditText etRePassword = null;
	EditText etServerAddr = null;

	Socket s = null;
	BufferedWriter dout = null;
	BufferedReader din = null;

	// 用来在主UI线程和socket通信线程之间传递信息，进而刷新界面
	@SuppressLint("HandlerLeak")
	public Handler myhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				Bundle bundle = msg.getData();
				if (bundle.getString("register_result").equals("true")) {
					Intent InfoViewintent = new Intent(RegisterActivity.this,AndroidBeControledActivity.class);
					startActivity(InfoViewintent);
					// 启动后台service，负责继续通信
					Toast.makeText(RegisterActivity.this, "启动Service成功",
							Toast.LENGTH_SHORT).show();
					Intent intent = new Intent(RegisterActivity.this,
							SocketService.class);
					intent.putExtra("userName", etUserName.getText().toString());
					startService(intent);
					finish();
				} else if (bundle.getString("register_result").equals("false")) {
					Toast.makeText(RegisterActivity.this, "注册失败，用户名已存在",
							Toast.LENGTH_SHORT).show();

				} else if (bundle.getString("register_result").equals("error")) {
					Toast.makeText(RegisterActivity.this, "连接服务器失败，请检查IP或者服务器未打开",
							Toast.LENGTH_SHORT).show();
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					s = null;// IP地址错误，置空socket
				}

			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		btRegister = (Button) findViewById(R.id.btRegister);
		btExit = (Button) findViewById(R.id.btExit);
		etUserName = (EditText) findViewById(R.id.etUserName);
		etPassword = (EditText) findViewById(R.id.etPassword);
		etRePassword = (EditText) findViewById(R.id.etRePassword);
		etServerAddr = (EditText) findViewById(R.id.etServerAddr);
		
		// 注册按钮响应事件
		btRegister.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final String username = etUserName.getText().toString();
				final String userkey = etPassword.getText().toString();
				String userre_key = etRePassword.getText().toString();
				final String serverip = etServerAddr.getText().toString();
				
				//确保所有输入内容不为空
				if (username.trim().equals("")) {
					etUserName.setError("请输入用户名！");
					return;
				} else if (userkey.trim().equals("")) {
					etPassword.setError("请输入密码！");
					return;
				} else if (serverip.trim().equals("")) {
					etServerAddr.setError("请输入确认密码！");
					return;
				} else if (userre_key.trim().equals("")) {
					etRePassword.setError("请输入服务器IP！");
					return;
				}
				// 两次输入密码不一致，则返回
				if (!userkey.equals(userre_key)) {
					etRePassword.setError("两次输入密码不一致！");
					return;
				}
				if (!isNetworkConnected(RegisterActivity.this)) {
					Toast.makeText(RegisterActivity.this, "网络未连接，请检查重试",
							Toast.LENGTH_SHORT).show();
				} else {
					// 新起线程负责通信
					new Thread() {
						@Override
						public void run() {
							try {
								Message msg = new Message();
								msg.what = 1;
								Bundle bundle = new Bundle();
								bundle.clear();

								if (s == null) {
									s = new Socket();
									s.connect(new InetSocketAddress(serverip,
											8888), 2 * 1000);
									// 使用单例来实现在多个activtiy中使用socket
									MySocket ss = MySocket.getSingleInstance();
									ss.setSocket(s);

								}
								
								din = new BufferedReader(new InputStreamReader(
										s.getInputStream()));
								dout = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"utf-8"));

								//封装注册信息为json数据
								JSONObject json = new JSONObject();
								json.put("fromUser", username);
								json.put("password", userkey);
								json.put("operation", "register");
								json.put("userType", "slave");
								json.put("toUser", "server");
								json.put("msgContent", "null");
								dout.write(json.toString() + "\n");
								dout.flush();

								String result;
								result = din.readLine();
								// 把返回信息发送给UI线程
								bundle.putString("register_result", result);
								msg.setData(bundle);
								myhandler.sendMessage(msg);

							} catch (Exception e) {
								Log.e("reconnect error", e.toString());
								Message Errormsg = new Message();
								Errormsg.what = 1;
								Bundle bundle = new Bundle();
								bundle.clear();
								bundle.putString("msg", "error");
								Errormsg.setData(bundle);
								myhandler.sendMessage(Errormsg);
							}
						}
					}.start();
				}// else

			}
		});
		// 退出返回按钮响应事件
		btExit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	/*
	 * 检查网络连接情况
	 */
	public boolean isNetworkConnected(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager
					.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				return mNetworkInfo.isAvailable();
			}
		}
		return false;
	}
}
