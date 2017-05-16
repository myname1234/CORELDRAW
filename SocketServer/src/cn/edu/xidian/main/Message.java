package cn.edu.xidian.main;

/**
 * 服务器端接收的消息类
 * @author Administrator
 *
 */

public class Message {
	private String fromUser = null;     //消息发送方（用户名）
	private String password = null;     //密码
	private String userType = null;     //用户类型（控制端/被控端）
	private String msgContent = null;   //消息内容（在这里我用来携带对端用户名）
	private String operation = null;    //操作
	private String toUser = null;       //消息接受方
	public String getFromUser() {
		return fromUser;
	}
	public void setFromUser(String fromUser) {
		this.fromUser = fromUser;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
	public String getToUser() {
		return toUser;
	}
	public void setToUser(String toUser) {
		this.toUser = toUser;
	}
	
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	public String getMsgContent() {
		return msgContent;
	}
	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}
	
	@Override
	public String toString() {
		return "Message [fromUser=" + fromUser + ", password=" + password + ", userType=" + userType + ", msgContent="
				+ msgContent + ", operation=" + operation + ", toUser=" + toUser + "]";
	}
	public Message(String fromUser, String password, String userType, String msgContent, String operation,
			String toUser) {
		super();
		this.fromUser = fromUser;
		this.password = password;
		this.userType = userType;
		this.msgContent = msgContent;
		this.operation = operation;
		this.toUser = toUser;
	}
	public Message() {
		super();
	}
	
	
	
}
