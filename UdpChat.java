// message content cannot contain "#"
// message content cannot contain ","
// TODO fix these if have time
import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


//****************************** Server Class *********************************
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

	Queue<String> messageQ = new LinkedList<String>();

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

	public String get_time_now(){
		DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date dateobj = new Date();
		System.out.println(df.format(dateobj));
		return df.format(dateobj).toString();
	}

	Hashtable<String,ArrayList<String>> off_line_msgs = new Hashtable<String,ArrayList<String>>();
	// <nick-name,[msg1,msg2,...]>
	// msg_str format: off_m#<from_usr>#<to_usr>#<msg_str>#<time>

	// preparation for ping dead
	boolean pingACKstatus = false;
	public void gotPingACK(){
		pingACKstatus = true;
	}
	public void backPingACK(){
		pingACKstatus = false;
	}

	public void register(String info) throws Exception {
		// do registration, or deal with off-line msgs
		System.out.println("in register()");
		String[] in = info.split("#");
		if(in.length==3){ // registration
			System.out.println(in[0]);
			System.out.println(in[1]);
			System.out.println(in[2]);
			String nick_name = in[0];
			String clnt_ip = in[1];
			String clnt_port = in[2];
			String clnt_status = new String("on");
			if(clients.containsKey(nick_name)){
				//
				if(clients.get(nick_name).get(2).equals("off")){
					// this one comes back
					clients.get(nick_name).set(2,"on");
					clients.get(nick_name).set(0,clnt_ip);
					clients.get(nick_name).set(1,clnt_port);
					broadcast();
					// check if there are off line mesg for this one
					System.out.println(off_line_msgs.get(nick_name).toString());
					if(off_line_msgs.containsKey(nick_name)){
						try{
							send("offMsgSending#"+"[You have messages]", clnt_ip, Integer.valueOf(clnt_port));
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						ArrayList<String> msg2send = off_line_msgs.get(nick_name);
						off_line_msgs.remove(nick_name);
						for(String str : msg2send){
							try{
								send("offMsgSending#"+str, clnt_ip, Integer.valueOf(clnt_port));
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				else {	// clients has this usr, change ip and port
					//
					clients.get(nick_name).set(2,"on");
					clients.get(nick_name).set(0,clnt_ip);
					clients.get(nick_name).set(1,clnt_port);
					broadcast();
				}
			}
			else{
				clients.put(nick_name,new ArrayList(Arrays.asList(clnt_ip,clnt_port,clnt_status)));
				// broadcast to every user
				this.broadcast();
			}
		}
		else if(in.length==4 && in[0].equals("off_m")){
			// if this is a off-line message
			// save this message to the according table entry
			// msg_str format: off_m#<from_usr>#<to_usr>
			// save format: <from_usr>: <time_stamp> <content>
			String from_usr = in[1];
			String to_usr = in[2];
			String msg_str = in[3];
			String to_usr_status = clients.get(to_usr).get(2);

			if(to_usr_status.equals("on")) {
				// to_usr is on, ping the to_usr to see if "dead" usr
				backPingACK();
				send("pingDead", clients.get(to_usr).get(0),
									Integer.valueOf(clients.get(to_usr).get(1)));
				try{
					Thread.sleep(500);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				if(pingACKstatus){
					// the usr is alive send erro mesg
					backPingACK();
					send("offMsgERR#"+to_usr, clients.get(from_usr).get(0),
										Integer.valueOf(clients.get(from_usr).get(1)));
				}
				else{
					// the usr is dead for good, change status to off
					clients.get(to_usr).set(2, "off");
					broadcast();
				}
			}
			to_usr_status = clients.get(to_usr).get(2);
			if(to_usr_status.equals("off")){
				// to_usr is off save this offline mesg
				String msg_new = from_usr+":  "+get_time_now()+"  "+msg_str.split(":\\s+")[1];
				if(off_line_msgs.containsKey(to_usr)){
					off_line_msgs.get(to_usr).add(msg_new);
				}
				else{
					off_line_msgs.put(to_usr, new ArrayList<String>(Arrays.asList(msg_new)));
				}
				System.out.println("off line msg table updated!");
				System.out.println(off_line_msgs.toString());
				// and send offMsgACK
				send("offMsgACK", clients.get(from_usr).get(0),
									Integer.valueOf(clients.get(from_usr).get(1)));
			}

		}
		else if(in.length==2 && in[0].equals("off_l")) {	// if receive deregistration:
			// msg_str format off_l#<usr_name>
			// clients table format: <nick-name,[ip,port,status]>
			String name = in[1];
			if(clients.containsKey(name)){
				clients.get(name).set(2,"off");
				String back_ip = clients.get(name).get(0);
				int back_port = Integer.valueOf(clients.get(name).get(1));
				send("offACK", back_ip, back_port); // send ACK
				broadcast();
			}
			// deregister and reregister
		}

		System.out.println("after in register()");
	}

	public String recv_register() throws Exception {
		// to recive message from client, including registration and off-line message
		// return as string
		byte[] buf = new byte[1024];
		// System.out.println("before in recv_register()");
	    DatagramPacket dp = new DatagramPacket(buf, 1024);
		// System.out.println("in the recv_register()");
	    ds.receive(dp);
		// System.out.println("after in the recv_register()");
	    String info = new String(dp.getData(), 0, dp.getLength());
		System.out.println(info);
		return info;
		// this.register(info);
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

class serverReceiver implements Runnable {	// keeps receiving from socket and put msg into messageQ
										// synchronized
	UdpChatServer server;
	int mutex;
	public serverReceiver(UdpChatServer cc, int mm){
		this.server = cc;
		this.mutex = mm;
	}

	@Override
	public void run(){
		while(true){
			String msg = null;
			try{
				msg = server.recv_register();
				// System.out.println("in receiver, received message is "+ msg);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if(msg!=null){
				synchronized(server.messageQ){
					server.messageQ.add(msg);
					server.messageQ.notify();
				}
			}
		}
	}
}

class serverHandler implements Runnable {		// keeps reading from messageQ

	UdpChatServer server;
	int mutex;
	public serverHandler(UdpChatServer cc, int mm){
		this.server = cc;
		this.mutex = mm;
	}

	@Override
	public void run(){

		while(true){

			if(server.messageQ.isEmpty()){
				synchronized(server.messageQ){
					try{
						server.messageQ.wait();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
			while(!server.messageQ.isEmpty()){
				System.out.println("I am in while!");
				// System.out.println("printer keeps working synchronized!");
				// synchronized (client.messageQ){
				// System.out.println("Get in to while loop in printer!");

				String msg = server.messageQ.poll();
				if(msg.equals("pingACK")){
					// TODO still alive send back offline mesg request err
					server.gotPingACK();
				}
				else{
					// TODO ordinary register message
					try{
						server.register(msg);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}






//****************************** Client Class *********************************
class UdpChatClient{

	Hashtable<String,ArrayList<String>> clients = new Hashtable<String,ArrayList<String>>();

	Queue<String> messageQ = new LinkedList<String>();

	String nick_name;
	int port;
	String server_ip;
	int server_port;
	DatagramSocket ds;
	SocketAddress sock_addr;
	// initialize
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

		sock_addr = ds.getLocalSocketAddress();

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
			String tmp = strs[i+1].replace("]","");
			String[] ipNport = tmp.split(",");
			clients.put(nick_name,new ArrayList<String>(Arrays.asList(ipNport)));
		}
		System.out.println(clients.toString());

	}

	// 从socket 中读一个 packet出来， 阻塞函数
	// 若来自 server 则更新clients table 并返回 null
	// 若来自 其他client 则 return msg as a String
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
			if(str.equals("offACK") || str.equals("offMsgACK") ||
				str.startsWith("offMsgERR#") || str.startsWith("offMsgSending#") ||
				str.equals("pingDead"))
			{
				return str;
			}
			else{
				updateClients(str);
				System.out.println(">>> [Client table updated.]");
				return null;
			}
		}

		// System.out.println("in recv(): Message from other client!");

		return str;
	}
	/* unused
	public String read_msg_from_Q() {
		String res;
		synchronized(messageQ){
			if(!messageQ.isEmpty()){
				res = messageQ.poll();
			}
			else
				res = null;
		}
		return res;
	}
	*/

	public  void send(String content, String ip_str, int port) throws Exception{
		InetAddress ip = InetAddress.getByName(ip_str);
		DatagramPacket dp = new DatagramPacket(content.getBytes(), content.length(), ip, port);
		ds.send(dp);
	}

	// preparation for send_P2P()
	boolean ack_status = false;
	void ackGot(){
		ack_status = true;
	}
	void ackBck(){
		ack_status = false;
	}

	public void send_P2P(String content, String user_recver) {
		// System.out.println("in send_P2P: user_recver is : "+user_recver);
		String ip_str = this.clients.get(user_recver).get(0);
		int port_num = Integer.valueOf(this.clients.get(user_recver).get(1));
		try{
			this.send(content,ip_str,port_num);
			// System.out.println("in send_P2P(): message sent to other client!");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		// wait 500 msec for ack

		//String wait = new String("NAK");

		TimerTask task = new TimerTask() {
      		@Override
      		public void run() {
				System.out.println("Waiting for ACK");
	        	// task to run goes here
				while(!ack_status){}		// wait for notifying
				// got an ACK
				// System.out.println("ack status is " + ack_status);
	      	}
    	};

		// System.out.println("before timer starts ack status is " + ack_status);
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
		// System.out.println("after timer ends ack status is " + ack_status);

		// time out
		// check ack_status
		if(ack_status){	// got ack
			// System.out.println("ack status is " + ack_status);
			ackBck();
			System.out.println("[Message received by " + user_recver + ".]\n>>> ");
		}
		else { // NAK
			timer_4_ACK.cancel();
			// System.out.println("Timer canceled!");
			// send to server as an off-line message
			// format: off_m#<from_usr>#<to_usr>#<msg_str>
			try{
				this.send("off_m#"+ this.nick_name + "#" + user_recver + "#" + content,
							this.server_ip, this.server_port);
			}
			catch (Exception e) {
				System.out.println(e);
				System.exit(1);
			}
			System.out.print("[No ACK from " + user_recver +", message sent to server.]\n>>> ");

		}

	}

	public String getIP() {
		// http://hanchaohan.blog.51cto.com/2996417/793377
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

	 // preparation for deRegister
	 boolean offACK_status = false;
	 void gotOffACK(){
		 offACK_status = true;
	 }
	 void backOffACK(){
		 offACK_status = false;
	 }

	 public void deRegister() {
		long delay = 0;
     	long inteval = 500; //msec

		for(int i = 0; i<5 ;i++){
			System.out.println(i);
			System.out.println("offACK_status is "+String.valueOf(offACK_status));

			try{	// send to server
   			 	send("off_l#"+nick_name, server_ip, server_port );
   		 	}
	   		 catch (Exception e) {
	   		 	e.printStackTrace();
	   		}
			try{
				Thread.sleep(inteval);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if(offACK_status) break;

		}
		if(offACK_status){
			backOffACK();
			System.out.println(" [You are Offline. Bye.]");
			System.out.print(">>>");
		}
		else{
			System.out.print("[Server not responding]\n>>> [Exiting]\n");
			System.exit(0);
		}
     	// schedules the task to be run in an interval

	 }

}

// sender and receiver and printer threads
class sender implements Runnable { // including sending deRegisteration to server
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
			String[] usr_in = line.split("\\s+",4);

			for(int i =0;i<usr_in.length;i++){
				System.out.println(usr_in[i]);
			}
			// System.out.println(usr_in[0]);
			if(usr_in[0].equals("send")){

				String name = usr_in[1]; 	// intend receiver name
				String message = usr_in[2];	// intend sending msg

				System.out.println(client.clients.toString());
				System.out.println("intedn status is "+client.clients.get(name).get(2));
				String intend_status = client.clients.get(name).get(2);
				System.out.println("intedn status equals on? : "+ intend_status.equals("on"));
				System.out.println(intend_status+intend_status+intend_status);
				if(client.clients.get(name).get(2).equals("on")){
				// if(true){
					System.out.println("get into try send_P2P");
					// if the intend receiver is online send it by P2P
					try{
						client.send_P2P(client.nick_name+":  "+message, name);
					}
					catch(Exception ex){
						ex.printStackTrace();
					}
				}
				else if(client.clients.get(name).get(2).equals("off")){
					// the intend receiver is offline send it to server as offline msg
					try{
						client.send("off_m#"+ client.nick_name + "#" + name + "#" +
									client.nick_name + ":  " + message,
									client.server_ip, client.server_port);
					}
					catch (Exception e) {
						System.out.println(e);
						System.exit(1);
					}

				}

			}
			else if(usr_in[0].equals("dereg")){ //
				if(usr_in.length==1 || usr_in[1].equals(client.nick_name)){
					client.deRegister();
				}
				else{
					System.out.println("You cannot dereg user other than youself...");
					System.out.print(">>> ");
				}
			}
			else if(usr_in[0].equals("reg")){
				// System.out.println("in reg section!");
				if(usr_in.length==1 || usr_in[1].equals(client.nick_name)){
					try{
						client.send(client.nick_name+"#"+client.getIP()+"#"+client.port,
									client.server_ip,client.server_port);
					}
					catch (Exception e) {
						e.printStackTrace();
					}

				}
			}
			else{
				System.out.println("Usage: send <name> <message>");
			}
			System.out.print("sender>>> ");
		}
	}
}

class receiver implements Runnable {	// keeps receiving from socket and put msg into messageQ
										// synchronized
	UdpChatClient client;
	int mutex;
	public receiver(UdpChatClient cc, int mm){
		this.client = cc;
		this.mutex = mm;
	}

	@Override
	public void run(){
		while(true){
			String msg = null;
			try{
				msg = client.recv();
				// System.out.println("in receiver, received message is "+ msg);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if(msg!=null){
				synchronized(client.messageQ){
					client.messageQ.add(msg);
					client.messageQ.notify();
				}
			}
		}
	}
}

class printer implements Runnable {		// keeps reading from messageQ
										// if is ordinary message print out
										// if is ACK, wait until main notify
	UdpChatClient client;
	int mutex;
	public printer(UdpChatClient cc, int mm){
		this.client = cc;
		this.mutex = mm;
	}

	@Override
	public void run(){

		/*
		while(true){
			// for test
			System.out.println("Q now is like: "+client.messageQ.toString());
			try{
				Thread.sleep(2000);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		*/
		// System.out.println("printer starts working!");
		while(true){
			/* for testing
			System.out.println("Q empty? : "+ client.messageQ.isEmpty());
			try{
				Thread.sleep(2000);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			*/
			if(client.messageQ.isEmpty()){
				synchronized(client.messageQ){
					try{
						client.messageQ.wait();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
			while(!client.messageQ.isEmpty()){
				System.out.println("I am in while!");
				// System.out.println("printer keeps working synchronized!");
				// synchronized (client.messageQ){
				// System.out.println("Get in to while loop in printer!");

				// System.out.println("printer keeps working! while loop");
				// System.out.println("Q empty? : "+ client.messageQ.isEmpty());
				String msg = client.messageQ.poll();
				if(msg.equals("pingDead")){
					try{
						client.send("pingACK", client.server_ip, client.server_port);
						System.out.println("I send pingACK to server!");
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if(msg.equals("ACK")){	// ACK
					// System.out.println("Here in printer, got an ACK!");
					try{
						client.ackGot(); // set ack_status so send_P2P can act
						// client.messageQ.wait();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if(msg.equals("offACK")){	// offACK
					System.out.println("Got an offACK");
					try{
						client.gotOffACK();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if(msg.equals("offMsgACK")) {
					System.out.println("[Messages received by the server and saved]");
				}
				else if(msg.startsWith("offMsgERR")) {
					String err_usrname = msg.split("#")[1];
					System.out.println("[Client "+err_usrname+" exists!!]");
					System.out.println("[Maybe "+err_usrname+"\'s PC is down...]");
				}
				else if(msg.startsWith("offMsgSending#")){
					String offmsg = msg.split("#")[1];
					System.out.println(offmsg);
				}
				else { // ordinary message
					System.out.println(msg);
					String back_name = msg.split(":\\s+")[0];
					String back_ip = client.clients.get(back_name).get(0);
					int back_port = Integer.valueOf(client.clients.get(back_name).get(1));
					try{
						client.send("ACK", back_ip, back_port);
						// System.out.println("in printer: ACK sent to back_ip: "+back_ip+", back_port"+back_port);
					}
					catch (Exception e) {
						e.printStackTrace();
					}

					// System.out.print("ACK sent printer>>> ");
				}

			}
		}
	}
}


//***************************     MAIN      ************************************//
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
			  int mutex = 0;
			  server.printInfo();
			  //server.send("Hello!","127.0.0.1",8081);

			  Thread t_serverReceiver = new Thread(new serverReceiver(server, mutex));
			  Thread t_serverHandler = new Thread(new serverHandler(server, mutex));

			  t_serverReceiver.start();
			  t_serverHandler.start();

			  t_serverReceiver.join();
			  t_serverHandler.join();

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
			  Thread t_printer = new Thread(new printer(client,mutex));
			  t_sender.start();
			  t_receiver.start();
			  t_printer.start();

			  t_sender.join();
			  t_receiver.join();
			  t_printer.join();
			  client.destroy();
		  }
	  }

    }
}
