import java.net.*;
import java.util.*;

class UdpChatServer{
	Hashtable<String,ArrayList<String>> clients = new Hashtable<String,ArrayList<String>>();
	// <nick-name,[ip,port,status]>
	int port;
	DatagramSocket ds;
	UdpChatServer(int portNumber) {
		port = portNumber;
		try{
			ds = new DatagramSocket(port);
		}
		catch (Exception e) {
			System.out.println(String.valueOf(e));
			System.exit(1);
		}

	}

	public  void send(String content, String ip_str, int client_port) throws Exception{
		InetAddress ip = InetAddress.getByName(ip_str);
		DatagramPacket dp = new DatagramPacket(content.getBytes(), content.length(), ip, client_port);
		ds.send(dp);
	}

	public void broadcast() throws Exception{
		System.out.println("in broadcast()");
		if(clients.isEmpty()){
			// do nothing
			System.out.println("in broadcast(): clients is empty");
		}
		else{
			Enumeration users = clients.keys();
			while(users.hasMoreElements()){
				String nick_name = new String(String.valueOf(users.nextElement()));
				String user_status = clients.get(nick_name).get(2);
				if(user_status.equals("off"))
					continue;
				System.out.println(nick_name);
				String user_ip = clients.get(nick_name).get(0);
				System.out.println(user_ip);
				int user_port = Integer.valueOf(clients.get(nick_name).get(1));
				System.out.println(user_port);
				System.out.println(clients.toString());
				try{
					this.send(clients.toString(),user_ip,user_port);
					System.out.println("in broadcast(): sent to is user_port"+user_port);
				}
				catch (Exception e) {
					System.out.println(String.valueOf(e));
					System.exit(1);
				}

			}
			System.out.println("after in broadcast()");
		}
	}

	public void register(String info) throws Exception {
		System.out.println("in register()");
		String[] in = info.split("#");
		System.out.println(in[0]);
		System.out.println(in[1]);
		System.out.println(in[2]);
		String nick_name = in[0];
		String clnt_ip = in[1];
		String clnt_port = in[2];
		String clnt_status = new String("on");
		if(clients.containsKey(nick_name)){
			// TODO send nak
		}
		else{
			clients.put(nick_name,new ArrayList(Arrays.asList(clnt_ip,clnt_port,clnt_status)));
			// TODO send ack
			this.broadcast();
		}
		System.out.println("after in register()");
	}

	public void recv_register() throws Exception {
		byte[] buf = new byte[1024];
		System.out.println("before in recv_register()");
	    DatagramPacket dp = new DatagramPacket(buf, 1024);
		System.out.println("in the recv_register()");
	    ds.receive(dp);
		System.out.println("after in the recv_register()");
	    String info = new String(dp.getData(), 0, dp.getLength());
		System.out.println(info);
		this.register(info);
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

	Hashtable<String,ArrayList<String>> clients = new Hashtable<String,ArrayList<String>>();

	String nick_name;
	int port;
	String server_ip;
	int server_port;
	DatagramSocket ds;
	UdpChatClient(int portNumber, String serverIP, int serverPort, String nname) throws Exception{
		server_ip = serverIP;
		port = portNumber;
		server_port = serverPort;
		nick_name = nname;
		try{
			ds = new DatagramSocket(port);
		}
		catch (Exception e) {
			System.out.println(String.valueOf(e));
			System.exit(1);
		}

		System.out.println("Client info: nick_name: "+nick_name+" serverIp "+server_ip+" server_port "+server_port+" client_port "+port);
	}

	public void destroy(){
		ds.close();
	}

	public void updateClients (String str){
		// formmat: {jiajun=[160.39.141.140, 8082], peter=[160.39.141.140, 8081], haha=[160.39.141.140, 8083]}
		clients.clear();
		str = str.replace("{","");
		str = str.replace("}","");
		str = str.replaceAll("\\s","");
		//String [] strs = str.split("=[|],");
		System.out.println(str);
		String [] strs = str.split("\\],|=\\[");

		for(int i =0;i<strs.length;i=i+2){
			String nick_name = strs[i];
			String tmp = strs[i+1];
			String[] ipNport = tmp.split(",");
			clients.put(nick_name,new ArrayList<String>(Arrays.asList(ipNport)));
		}

	}

	public String recv() throws Exception{
		byte[] buf = new byte[1024];
	    DatagramPacket dp = new DatagramPacket(buf, 1024);
	    ds.receive(dp);
		System.out.println("Receiving!");
		String str = new String(dp.getData(), 0, dp.getLength());
		//System.out.println(str);
		System.out.println(dp.getAddress().toString());
		System.out.println(dp.getPort());
		if(dp.getAddress().toString().replace("/","").equals(server_ip) && dp.getPort()==this.server_port){
			// this packet is from server
			updateClients(str);
			System.out.println(">>> [Client table updated.]");
		}
		else if( !str.equals("ACK") ) {
			// this packet is from other client
			// TODO send ack
			System.out.println(str);
			String[] strs = str.split(":",2);
			// for(int i =0;i<strs.length;i++) System.out.println(strs[i]);
			String back_name = strs[0];
			String back_ip = clients.get(back_name).get(0);
			int back_port = Integer.valueOf(clients.get(back_name).get(1));
			System.out.println("sending ack to : " + back_ip + "  "+back_port);
			try{
				send("ACK", back_ip, back_port);
				System.out.println("sent ack to : " + back_ip + "  "+back_port);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return str;

	}

	public  void send(String content, String ip_str, int port) throws Exception{
		InetAddress ip = InetAddress.getByName(ip_str);
		DatagramPacket dp = new DatagramPacket(content.getBytes(), content.length(), ip, port);
		ds.send(dp);
	}

	// preparation for send_P2P()
	String ack_status = new String("NAK");
	void ackGot(){
		ack_status = "ACK";
	}
	void ackBck(){
		ack_status = "NAK";
	}

	public void send_P2P(String content, String user_recver) {
		String ip_str = this.clients.get(user_recver).get(0);
		int port_num = Integer.valueOf(this.clients.get(user_recver).get(1));
		try{
			this.send(content,ip_str,port_num);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		// wait 500 msec for ack

		//String wait = new String("NAK");
		ackBck();

		TimerTask task = new TimerTask() {
      		@Override
      		public void run() {
				System.out.println("Waiting for ACK");
	        	// task to run goes here
				String wait = new String("NAK");
				try{
					System.out.println("Waiting for ACK in try");
					// TODO send signal to thread to pause recv in thread
					interuptRecvThread()
					// this.notify();
					wait = recv();
					System.out.println("wait is "+ wait);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				if(wait.equals("ACK")) {
					System.out.println("ack status is " + ack_status);
					ackGot();
					System.out.println("ack status is " + ack_status);
					// TODO cancel sleeper
				}
	        	System.out.println("Hello from the timer task!!!");
	      	}
    	};
    	Timer timer_4_ACK = new Timer();
    	long delay = 0;
    	long inteval = 500; //msec
    	// schedules the task to be run in an interval
    	timer_4_ACK.schedule(task, delay);
		try{
			Thread.sleep(inteval);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// TODO time out send to server
		if(ack_status.equals("ACK")){
			System.out.println("wait finished ");
			System.out.println("ack status is " + ack_status);
			ackBck();
			System.out.println("ack status is " + ack_status);
			System.out.println("[Message received by " + user_recver + ".+]\n>>> ");
		}
		else { // NAK
			timer_4_ACK.cancel();
			// send to server
			try{
				this.send(this.nick_name + "#" + user_recver + "#" + content,
							this.server_ip, this.server_port);
			}
			catch (Exception e) {
				System.out.println(e);
				System.exit(1);
			}

			System.out.println("[No ACK from " + user_recver +", message sent to server.]\n>>> ");
		}

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

// sender and receiver threads
class sender implements Runnable {
	UdpChatClient client;
	int mutex;
	public sender(UdpChatClient cc, int mm){
		this.client = cc;
		this.mutex = mm;
	}

	@Override
	public void run(){
		while(true){
			Scanner sc = new Scanner(System.in);
			String line = sc.nextLine();
			System.out.println(line);
			String[] usr_in = line.split("\\s+",3);
			for(int i =0;i<usr_in.length;i++){
				System.out.println(usr_in[i]);
			}
			if(usr_in[0].equals("send")){
				String name = new String(usr_in[1]);
				String message = new String(usr_in[2]);
				System.out.println("Sending");
				System.out.println(client.clients.toString());
				try{
					client.send_P2P(client.nick_name+":  "+message, name);
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
			else{
				System.out.println("Usage: send <name> <message>");
			}
			System.out.print("sender>>> ");
		}
	}
}

class receiver implements Runnable {
	UdpChatClient client;
	int mutex;
	public receiver(UdpChatClient cc, int mm){
		this.client = cc;
		this.mutex = mm;
	}

	@Override
	public void run(){
		while(client.waiting4ack!=true){
			synchronized(client){
				try{
						client.recv();
				}
				catch(Exception ex){
					if(ex.equals(SocketTimeoutException)){
						System.out.println("This receiver is interupted by wating for receiving ACK!...")
						System.out.println("Start wait() until being notified!...");
						client.wait();
						System.out.println("I am notified, start receiving for regular massage!...");
					}
				}
			}
			System.out.print("receiver>>> ");
		}
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
			  UdpChatClient client = new UdpChatClient(client_port,server_ip,server_port,nick_name);
			  System.out.println("check client ds status:"+String.valueOf(client.ds.getInetAddress())+client.ds.getPort());
			  // send register message to server
			  client.send(nick_name+"#"+client.getIP()+"#"+client_port,server_ip,server_port);
			  // System.out.println(client.recv());
			  System.out.print(">>> [Welcome. You are registered.]\n");

			  System.out.print(">>> ");
			  int mutex = 1;
			  Thread t_sender = new Thread(new sender(client,mutex));
			  Thread t_receiver = new Thread(new receiver(client,mutex));
			  t_sender.start();
			  t_receiver.start();

			  t_sender.join();
			  t_receiver.join();
			  client.destroy();
		  }
	  }

    }
}
