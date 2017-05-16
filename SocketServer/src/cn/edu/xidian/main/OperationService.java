package cn.edu.xidian.main;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTextArea;

/**
 * 对消息中的操作进行相应的处理
 * @author Administrator
 *
 */

public class OperationService {
	
	private DataBaseUtil dbUtil = new DataBaseUtil();
	
	/*
	 * 注册操作处理
	 */
	public String register(String userName, String password, int socketId ,JTextArea show) {
		String result = null;
		if(!dbUtil.checkUser(userName)) {
			dbUtil.addUser(userName, password);
			result = "true";
			show.append(getTime(System.currentTimeMillis()) + "---"
			+ "用户：" + userName + "注册成功, result = " +result + "\n");
		} else {
			dbUtil.deleteOnlineUser(userName);
			result = "false";
			show.append(getTime(System.currentTimeMillis()) + "---" 
			+ "用户：" + userName + "已存在，注册失败，result = " + result + "\n");
		}
		return result;
	}
	
	/*
	 * 登录操作处理
	 */
	public String login(String userName, String password,String userType, int socketId ,JTextArea show) {
		String result = null;
		if (dbUtil.checkLogin(userName,password) ) {
			result = "success";
			show.append(getTime(System.currentTimeMillis()) + "---" 
			+ "用户：" + userName + "登录成功, result = " + result + "\n");
		} else {
			dbUtil.deleteOnlineUser(userName);
			result = "failed";
			show.append(getTime(System.currentTimeMillis()) + "---"
			+ "用户名或密码错误,result = " + result + "\n");
		}
		return result;
	}
	
	/*
	 * 连接操作处理
	 */
	public String connect(String toUser, JTextArea show) {
		String result = null;
		if (dbUtil.isOnline(toUser)) {
			result = "online";
			show.append(getTime(System.currentTimeMillis()) + "---"
			+ "用户：" + toUser + "在线 , result = " + result + "\n");
		} else {
			result = "offline";
			show.append(getTime(System.currentTimeMillis()) + "---" 
			+ "用户：" + toUser + "离线 , result = " + result + "\n");
		}
		return result;
	}
	
	/*
	 * 获取系统当前时间
	 */
	private String getTime(long millTime) {
		Date d = new Date(millTime);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(d);
	}

	public String queryState(String msgContent, JTextArea showArea) {
		String result = dbUtil.queryState(msgContent);
		showArea.append("被控制用户屏幕状态是：" + result + "\n");
		return result;
	}
}
