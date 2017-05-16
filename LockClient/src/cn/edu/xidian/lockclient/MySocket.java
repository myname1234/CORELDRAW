package cn.edu.xidian.lockclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * 单例socket，确保一个类只有一个socket，并且提供一个全局访问点
 * @author Administrator
 *
 */

public class MySocket {
	
	public Socket socket;
	public BufferedWriter dout = null;
	public BufferedReader din = null;
	private static MySocket mySocket = null;
	
	private MySocket(){}   //私有构造方法，不能被外部访问
	
	/*
	 * 当两个并发线程访问同一个对象object中的这个synchronized(this)同步代码块时,
	 * 一个时间内只能有一个线程得到执行，保证只能创建一个socket
	 */
	public static synchronized MySocket getSingleInstance(){
		if(mySocket == null){
			mySocket = new MySocket();
		}
		return mySocket;
	}
	
	public Socket getSocket(){
		return this.socket;
	}
	
	public void setSocket(Socket s) throws IOException{
		this.socket = s;
		this.din = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
		this.dout = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"utf-8"));
	}
}
