import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//****************************** Client Class *********************************
public class UdpChatClient{

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

		// System.out.println("Client info: nick_name: "+nick_name+" serverIp "+server_ip+" server_port "+server_port+" client_port "+port);
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
		// String [] strs = str.split("=[|],");
		// System.out.println(str);
		String [] strs = str.split("\\],|=\\[");

		for(int i =0;i<strs.length;i=i+2){
			String nick_name = strs[i];
			String tmp = strs[i+1].replace("]","");
			String[] ipNport = tmp.split(",");
			clients.put(nick_name,new ArrayList<String>(Arrays.asList(ipNport)));
		}
		System.out.print("[Client table updated.]\n>>> ");
		printClinetsTable();

	}

	public void printClinetsTable(){
		// System.out.println(clients.toString());
		Enumeration enu = clients.keys();
		while(enu.hasMoreElements()){
			String usr_name = enu.nextElement().toString();
			System.out.print("[ "+usr_name + ": " + clients.get(usr_name).get(2)+" ]\t");
		}
		System.out.print("\n");
	}

	// 从socket 中读一个 packet出来， 阻塞函数
	// 若来自 server 则更新clients table 并返回 null
	// 若来自 其他client 则 return msg as a String
	public String recv() throws Exception{
		byte[] buf = new byte[1024];
	    DatagramPacket dp = new DatagramPacket(buf, 1024);
	    ds.receive(dp);
		// System.out.println("Receiving!");
		String str = new String(dp.getData(), 0, dp.getLength());

		// System.out.println(str);
		// System.out.println(dp.getAddress().toString());
		// System.out.println(dp.getPort());
		if(dp.getAddress().toString().replace("/","").equals(server_ip) && dp.getPort()==this.server_port){
			// this packet is from server
			if(str.equals("offACK") || str.equals("offMsgACK") ||
				str.startsWith("offMsgERR#") || str.startsWith("offMsgSending#") ||
				str.equals("pingDead") || str.equals("RegACK"))
			{
				// System.out.println(str);
				return str;
			}
			else{
				updateClients(str);
				// System.out.println(">>> [Client table updated.]");
				System.out.print(">>> ");
				return null;
			}
		}

		// System.out.println("in recv(): Message from other client!");

		return str;
	}

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
				// System.out.println("Waiting for ACK");
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
			System.out.println(">>> [Message received by " + user_recver + ".]");

		}
		else { // NAK
			timer_4_ACK.cancel();
			// System.out.println("Timer canceled!");
			// send to server as an off-line message
			// format: off_m#<from_usr>#<to_usr>#<msg_str>
			backOffACK();
			System.out.print(">>> [No ACK from " + user_recver +", message sent to server.]\n");
			for(int i = 0; i<5 ;i++){
				if(offACK_status) break;
				try{
				this.send("off_m#"+ this.nick_name + "#" + user_recver + "#" + content,
							this.server_ip, this.server_port);
				}
				catch (Exception e) {
					System.out.println(e);
					System.exit(1);
				}
				try{
					Thread.sleep(1000);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(offACK_status){
				backOffACK();
			}
			else{
				System.out.print(">>> [Server not responding]\n>>> [Exiting]\n");
				System.exit(0);
			}
		}

	}

	public String getIP() {
		// cite from http://hanchaohan.blog.51cto.com/2996417/793377
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
		backOffACK();
		for(int i = 0; i<5 ;i++){
			// System.out.println(i);
			// System.out.println("offACK_status is "+String.valueOf(offACK_status));

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
			System.out.println(">>> [You are Offline. Bye.]");

		}
		else{
			System.out.print(">>> [Server not responding]\n>>> [Exiting]\n");
			System.exit(0);
		}
     	// schedules the task to be run in an interval

	 }

	 public void Register() {
		long delay = 0;
     	long inteval = 500; //msec
		backOffACK();
		for(int i = 0; i<5 ;i++){
			// System.out.println(i);
			// System.out.println("offACK_status is "+String.valueOf(offACK_status));
			try{
				send(nick_name+"#"+getIP()+"#"+port,
							server_ip, server_port);
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
			System.out.print("[Welcome. You are registered.]\n");


		}
		else{
			System.out.print(">>> [Server not responding]\n>>> [Exiting]\n");
			System.exit(0);
		}
     	// schedules the task to be run in an interval

	 }


}
