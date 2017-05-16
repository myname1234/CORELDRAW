package cn.edu.xidian.lockclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.json.JSONObject;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * 被控制端界面
 * @author Administrator
 *
 */

public class AndroidBeControledActivity extends Activity {
	
	Button btExit = null;
	Button btActive = null;
	Socket serverSocket = null;
	BufferedWriter server_dout = null;
	String userName = null;
	
	// 用来激活设备管理器
	private DevicePolicyManager policyManager;
	private ComponentName componentName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_android_becontroled);
		
		btExit = (Button) findViewById(R.id.btExit);
		btActive = (Button) findViewById(R.id.btActive);
		policyManager = (DevicePolicyManager) getSystemService(AndroidBeControledActivity.DEVICE_POLICY_SERVICE);
		componentName = new ComponentName(this, MyReceiver.class);
		
		Intent intent = getIntent();
		userName = intent.getStringExtra("userName");
		
		//监听退出按钮点击事件
		btExit.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
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
							
							JSONObject json = new JSONObject();

							json.put("fromUser", userName);
							json.put("password", "null");
							json.put("operation", "exit");
							json.put("userType", "slave");
							json.put("toUser", "server");
							json.put("msgContent", "null");

							server_dout.write(json.toString() + "\n");
							server_dout.flush();

							
							
						}catch(Exception e){
							Log.e("reConnect_Error", e.toString());
							
						} 
					};
				}.start();
				finish();
				System.exit(0);
			}
		});
		
		//监听激活设备管理器按钮点击事件
		btActive.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!policyManager.isAdminActive(componentName)) {
					activeManager();// 启动设备管理器
					Toast.makeText(AndroidBeControledActivity.this, "启动设备管理器成功",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(AndroidBeControledActivity.this, "您已激活设备管理器",
							Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	// 使用隐式意图调用系统方法来激活指定的设备管理器
	private void activeManager() {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "远程锁屏");
		startActivity(intent);
	}
	
	// 解除绑定
	public void Bind(View v) {
		if (componentName != null) {
			policyManager.removeActiveAdmin(componentName);
			activeManager();
		}
	}
}
