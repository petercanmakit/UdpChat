import java.net.*;
import java.util.*;

class UdpChatServer{
	HashMap<String,ArrayList<String>> clients = new HashMap();
	// <nick-name,[ip,port,status]>
	int port;
	DatagramSocket ds;
	UdpChatServer(int portNumber) throws Exception{
		port = portNumber;
		ds = new DatagramSocket(port);
	}

	public  void send(String content, String ip_str, int client_port) throws Exception{
		InetAddress ip = InetAddress.getByName(ip_str);
		DatagramPacket dp = new DatagramPacket(content.getBytes(), content.length(), ip, client_port);
		ds.send(dp);
	}

	public void recv_register() throws Exception {
		byte[] buf = new byte[1024];
	    DatagramPacket dp = new DatagramPacket(buf, 1024);
	    ds.receive(dp);
	    String info = new String(dp.getData(), 0, dp.getLength());
		System.out.println(info);
	}

	public  void destroy(){
		ds.close();
	}

	public  String getIP() { // http://hanchaohan.blog.51cto.com/2996417/793377
          String ip;
          try {
               /**返回本地主机。*/
               InetAddress addr = InetAddress.getLocalHost();
               /**返回 IP 地址字符串（以文本表现形式）*/
               ip = addr.getHostAddress();
          } catch(Exception ex) {
              ip = "";
          }

          return ip;
     }

	 public  void printInfo() {
		 System.out.println("Server created:\nIpAddr:"+this.getIP()+"\nPort: "+port);
	 }

}

class UdpChatClient{

	int port;
	DatagramSocket ds;
	UdpChatClient(int portNumber) throws Exception{
		port = portNumber;
		ds = new DatagramSocket(port);
	}

	public void destroy(){
		ds.close();
	}

	public String recv() throws Exception{
		byte[] buf = new byte[1024];
	    DatagramPacket dp = new DatagramPacket(buf, 1024);
	    ds.receive(dp);
	    String str = new String(dp.getData(), 0, dp.getLength());
	    return str;
	}

	public  void send(String content, String ip_str, int port) throws Exception{
		InetAddress ip = InetAddress.getByName(ip_str);
		DatagramPacket dp = new DatagramPacket(content.getBytes(), content.length(), ip, port);
		ds.send(dp);
	}

	public String getIP() { // http://hanchaohan.blog.51cto.com/2996417/793377
          String ip;
          try {
               /**返回本地主机。*/
               InetAddress addr = InetAddress.getLocalHost();
               /**返回 IP 地址字符串（以文本表现形式）*/
               ip = addr.getHostAddress();
          } catch(Exception ex) {
              ip = "";
          }

          return ip;
     }

	 public void printInfo() {
		 System.out.println("Client created:\nIpAddr:"+this.getIP()+"\nPort: "+port);
	 }

}

public class UdpChat{

	public static void main(String[] args) throws Exception {
      // UdpChat <mode> <command-line arguments>
	  if(args.length<2){
		  System.out.println("Useage: UdpChat <mode> <command-line arguments>\nFor server: Useage: UdpChat -s <port>\nFor client: UdpChat -c <nick-name> <server-ip> <server-port> <client-port> \n");
		  System.exit(1);
	  }
	  String mode = args[0];
	  if(mode.equals("-s")){	// server mode
		  if(args.length!=2){
			  System.out.println("Useage: UdpChat -s <port>\n");
			  System.exit(1);
		  }
		  else{
			  int port = Integer.valueOf(args[1]);
			  System.out.println(port);
			  System.out.println("Server port is: "+port);
			  // start server with port
			  UdpChatServer server = new UdpChatServer(port);
			  // server created
			  server.printInfo();
			  //server.send("Hello!","127.0.0.1",8081);
			  while(true){
				  server.recv_register();
				  System.out.print(">>> ");
				  if(1!=1) break;
			  }
			  server.destroy();
		  }
	  }
	  else if(mode.equals("-c")){	//client mode
		  if(args.length!=5){
			  System.out.println("Useage: UdpChat -c <nick-name> <server-ip> <server-port> <client-port> \n");
			  System.exit(1);
		  }
		  else{
			  String nick_name = args[1];
			  String server_ip = args[2];
			  int server_port = Integer.valueOf(args[3]);
			  int client_port = Integer.valueOf(args[4]);
			  System.out.println("nick-name is "+nick_name+"\nserver-ip is "+server_ip+
			  					"\nserver-port is "+server_port+"\nclient-port is "+client_port);
			  // start client
			  UdpChatClient client = new UdpChatClient(client_port);
			  client.send(nick_name+"*"+client.getIP()+"*"+client_port,server_ip,server_port);
			  //System.out.println(client.recv());
			  System.out.print(">>> [Welcome. You are registered.]\n");
			  client.destroy();
		  }
	  }
	  // UdpChat -s <port> : Initiate the server process

	  // UdpChat -c <nick-name> <server-ip> <server-port> <client-port> : Initiate the client


    }
}
