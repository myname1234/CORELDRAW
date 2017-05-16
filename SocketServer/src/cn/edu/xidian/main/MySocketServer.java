package cn.edu.xidian.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.json.JSONObject;

/**
 * 服务器端主类，处理客户端传递过来的消息
 * @author Administrator
 *
 */

@SuppressWarnings("serial")
public class MySocketServer extends JFrame{

	private static final int SOCKET_PORT = 8888;  //端口号
	private ServerSocket serverSocket = null;
	private boolean flag = true;
	private int socketId;                        //用于区分不同的线程
	
	private DataBaseUtil dbUtil = new DataBaseUtil();
	private OperationService os = new OperationService();
	
	private ArrayList<Message> mMsgList = new ArrayList<Message>();  //接收消息的ArrayList集合
	private ArrayList<SocketThread> mThreadList = new ArrayList<SocketThread>(); //线程类的ArrayList集合
	
	private static JTextArea showArea = new JTextArea();
	
	/*
	 * 交互信息显示窗口
	 */
	public MySocketServer() {
		setTitle("服务器端交互信息显示界面");
		setSize(600, 600);
		setResizable(true);
		showArea.setEditable(false);
		JLabel timechange = new JLabel();
		add(timechange, BorderLayout.NORTH);
		JScrollPane jslp = new JScrollPane(showArea); // 加滚动条
		jslp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		showArea.setFont(new Font("宋体", Font.PLAIN, 15));
		showArea.setForeground(Color.black);
		
		add(jslp, BorderLayout.CENTER);
				
		// 添加窗口关闭事件
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int selection = JOptionPane.showConfirmDialog(MySocketServer.this,
						"您要退出吗？", "提示", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (selection == JOptionPane.OK_OPTION) {
					dbUtil.deleteAll();;// 关闭服务器时，清空OnLineUser表
					System.exit(0);
				}
				else {
					setVisible(true);
				}
			}
		});
		setLocationRelativeTo(null); // 居中显示
		setResizable(false);// 窗口不可调节大小
		setVisible(true);// 让容器可显示
	}
	
	public static void main(String[] args) {
		MySocketServer mySocketServer = new MySocketServer();
		mySocketServer.initSocket();
	}
	
	/*
	 * 监听socket连接并建立拥有不同socketId的线程，然后处理信息
	 */
	private void initSocket() {
		try {
			serverSocket = new ServerSocket(SOCKET_PORT);
			showArea.append(getTime(System.currentTimeMillis()) + "---" 
			+ "服务器已经启动，端口号：" + SOCKET_PORT 
			+ " IP: " + InetAddress.getLocalHost().getHostAddress() + "\n");
			startMessageThread();       //启动消息处理线程
			while(flag) {
				Socket clientSocket = serverSocket.accept();
				//建立拥有不同socketId的线程
				SocketThread socketThread = new SocketThread(clientSocket, socketId++);
				socketThread.start();
				mThreadList.add(socketThread);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	 * socket线程类
	 */
	public class SocketThread extends Thread {
		public Socket socket;
		public int mSocketId;
		public String userName;
		public String userType;
		public BufferedReader reader;
		public BufferedWriter writer;
		
		public SocketThread(Socket clientSocket, int socketId) {
			this.mSocketId = socketId;
			this.socket = clientSocket;
			showArea.append(getTime(System.currentTimeMillis()) + "---" 
			+ "用户id为：" + mSocketId + "上线了\n");
			InputStream inputStream;
			try {
				inputStream = socket.getInputStream();
				reader = new BufferedReader(new InputStreamReader(inputStream,"utf-8"));
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"utf-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			super.run();
			try{
				while (flag) {
					if (reader.ready()) {                  //接收客户端传来的消息，并存入消息ArrayList集合中
						String comeData = reader.readLine();
						//System.out.println(comeData);
						JSONObject msgJson = new JSONObject(comeData);
						Message msg = new Message();
						userName = msgJson.getString("fromUser");
						userType = msgJson.getString("userType");
						
						//将用户添加到在线表中（错误用户不用怕，在操作处理时会删除）
						if(!dbUtil.isOnline(userName)) {
							dbUtil.addOnlineUser(userName, userType, mSocketId, "on");
						}
						
						msg.setFromUser(msgJson.getString("fromUser"));
						msg.setOperation(msgJson.getString("operation"));
						msg.setPassword(msgJson.getString("password"));
						msg.setToUser(msgJson.getString("toUser"));
						msg.setMsgContent(msgJson.getString("msgContent"));
						msg.setUserType(msgJson.getString("userType"));
						mMsgList.add(msg);
						
						showArea.append(getTime(System.currentTimeMillis()) + "---" + 
						"用户："+ mSocketId + "--" + msg.getFromUser()+"向用户：" 
								+ msg.getToUser()+"发送的操作内容为："+msg.getOperation() + "\n");
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/*
	 * 消息处理线程
	 */
	public void startMessageThread() {
		new Thread() {
			@Override
			public void run() {
				super.run();
				try {
					while(flag) {
						if(mMsgList.size() > 0) {
							Message from = mMsgList.get(0);
							String operation = from.getOperation();
							String aimUser = from.getToUser();
							
							//控制端用户登录
							if(aimUser.equals("server") && operation.equals("login_master")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getFromUser().equals(toUser)) {
										String result = os.login(from.getFromUser(), from.getPassword(),
												from.getUserType(), toThread.mSocketId, showArea);
										BufferedWriter writer = toThread.writer;
										writer.write(result + "\n");
										writer.flush();
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "转发控制端用户登录结果消息成功\n");
										break;
									}
								}
							}
							
							//被控端用户登录
							if(aimUser.equals("server") && operation.equals("login_slave")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getFromUser().equals(toUser)) {
										String result = os.login(from.getFromUser(), from.getPassword(), 
												from.getUserType(), toThread.mSocketId, showArea);
										BufferedWriter writer = toThread.writer;
										writer.write(result + "\n");
										writer.flush();
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "转发被控制端用户登录结果消息成功\n");
										break;
									}
								}
							}
							
							//用户注册
							if(aimUser.equals("server") && operation.equals("register")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getFromUser().equals(toUser)) {
										String result = os.register(from.getFromUser(), 
												from.getPassword(), toThread.mSocketId, showArea);
										BufferedWriter writer = toThread.writer;
										writer.write(result + "\n");
										writer.flush();
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "转发注册用户结果消息成功");
										break;
									}
								}
							}
							
							//控制端检查被控端用户是否在线
							if(aimUser.equals("server") && operation.equals("connect")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getFromUser().equals(toUser)) {
										String result = os.connect(from.getMsgContent(), showArea);
										BufferedWriter writer = toThread.writer;
										writer.write(result + "\n");
										writer.flush();
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "转发被控制端用户在线结果消息成功\n");
										//dbUtil.addUserConnection(from.getFromUser(), from.getMsgContent());
										break;
									}
								}
							}
							
							if(aimUser.equals("server") && operation.equals("queryState")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getFromUser().equals(toUser)) {
										String result = os.queryState(from.getMsgContent(), showArea);
										BufferedWriter writer = toThread.writer;
										writer.write(result + "\n");
										writer.flush();
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "转发被控制端用户屏幕状态结果消息成功\n");
										break;
									}
								}
							}
							
							//锁屏
							if(aimUser.equals("server") && operation.equals("lock")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getMsgContent().equals(toUser)) {
										BufferedWriter writer = toThread.writer;
										writer.write("lock" + "\n");
										writer.flush();
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "转发锁屏消息成功\n");
										break;
									}
								}
							}
							
							//解锁
							if(aimUser.equals("server") && operation.equals("unlock")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getMsgContent().equals(toUser)) {
										BufferedWriter writer = toThread.writer;
										writer.write("unlock" + "\n");
										writer.flush();
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "转发解锁消息成功\n");
										break;
									}
								}
							}
							
							//客户端断开连接
							if(aimUser.equals("server") && operation.equals("exit")) {
								for(SocketThread toThread : mThreadList) {
									String toUser = dbUtil.queryBySocketId(toThread.mSocketId);
									if(from.getFromUser().equals(toUser)) {
										dbUtil.deleteOnlineUser(from.getFromUser());
										showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "删除在线用户" + from.getFromUser() + "成功\n");
										break;
									}
								}
							}
							
							//发送被控端屏幕开启状态
							if(aimUser.equals("server") && operation.equals("on")) {
								dbUtil.updateScreenState(from.getFromUser(), "on");
								showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "向服务器端发送屏幕状态：" + operation + "成功\n");
							}
							
							//向控制端发送被控制端屏幕关闭状态
							if(aimUser.equals("server") && operation.equals("off")) {
								dbUtil.updateScreenState(from.getFromUser(), "off");
								showArea.append(getTime(System.currentTimeMillis()) + "---" 
										+ "向服务器端发送屏幕状态：" + operation + "成功\n");
							}
							
							mMsgList.remove(0);
						}
						Thread.sleep(200);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	//获取当前系统时间
	private String getTime(long millTime) {
		Date d = new Date(millTime);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(d);
	}

}
