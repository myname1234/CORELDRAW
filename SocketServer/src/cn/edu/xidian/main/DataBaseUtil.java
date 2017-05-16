package cn.edu.xidian.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 服务器端与数据库交互的方法类
 * @author Administrator
 *
 */

public class DataBaseUtil {
	private static Connection connection = getConn();
    private static Statement smt = null;
	
    /*
     * 获取与数据库的连接
     */
	private static Connection getConn() {
		String driverName = "com.mysql.jdbc.Driver";	
		String dbURL = "jdbc:mysql://127.0.0.1:3306/socket_server";
		String userName = "root";
		String userPwd = "root";
		try {
			Class.forName(driverName);
			Connection dbConn;
			dbConn = DriverManager.getConnection(dbURL, userName, userPwd);
			return dbConn;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * 往数据库用户表添加用户，注册时调用
	 */
	public void addUser(String userName, String password) {
		String sql = "insert into all_users values(?,?)";
		
		try {
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userName);
			ps.setString(2, password);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	 * 检查数据库用户表中某个用户是否存在
	 */
	public boolean checkUser(String userName) {
		try {
			smt = connection.createStatement();
			String sql = "select * from all_users where userName='" + userName + "';";
			ResultSet rs = smt.executeQuery(sql);
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/*
	 * 判断某个用户用户名或密码是否正确，登录时调用
	 */
	public boolean checkLogin(String userName, String password) {
		try {
			String sql = "select * from all_users where userName='" 
					+ userName + "' and password= '" + password + "';";
			smt = connection.createStatement();
			ResultSet rs = smt.executeQuery(sql);
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/*
	 * 往在线用户表中添加用户
	 */
	public void addOnlineUser(String userName,String userType,int socketId, String screenState) {
		String sql = "insert into online_users values(?,?,?,?)";
		try {
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userName);
			ps.setString(2, userType);
			ps.setInt(3, socketId);
			ps.setString(4, screenState);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 更新数据库中屏幕状态
	 */
	public void updateScreenState(String userName, String screenState) {
		String sql = "update online_users set screenState='" + screenState + "' where userName='" + userName + "'";
		try {
			smt =connection.createStatement();
			smt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 删除在线用户表的用户，在断开连接时调用
	 */
	public void deleteOnlineUser(String userName) {
		String sql = "delete from online_users where userName='" + userName + "';";
		try {
			smt = connection.createStatement();
			smt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 删除所有在线用户，在服务器断开连接时调用
	 */
	public void deleteAll() {
		String sql = "delete from online_users";
		try {
			smt = connection.createStatement();
			smt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 通过socketId查询对应的用户名（不同的用户名建立socket连接时对应不同的socketId）
	 */
	public String queryBySocketId(int socketId) {
		String sql = "select userName from online_users where socketId=" + socketId;
		try {
			String userName = null;
			smt = connection.createStatement();
			ResultSet rs = smt.executeQuery(sql);
			while(rs.next()) {
				userName = rs.getString(1);
			}
			return userName;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * 判断用户是否在线
	 */
	public boolean isOnline(String userName) {
		String sql = "select * from online_users where userName='" + userName + "';";
		try {
			smt = connection.createStatement();
			ResultSet rs = smt.executeQuery(sql);
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	

	
	public String queryState(String userName) {
		String sql = "select screenState from online_users where userName='" + userName + "';";
		String state = null;
		try {
			smt = connection.createStatement();
			ResultSet rs = smt.executeQuery(sql);
			while(rs.next()) {
				state = rs.getString(1);
			}
			return state;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
