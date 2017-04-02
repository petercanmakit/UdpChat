import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


//****************************** Server Class *********************************
public class UdpChatServer{
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
			Enumeration<String> users = clients.keys();
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
					try{
						send("RegACK", clnt_ip, Integer.valueOf(clnt_port));
					}
					catch (Exception e) {
						e.printStackTrace();
					}
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
					try{
						send("RegACK", clnt_ip, Integer.valueOf(clnt_port));
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					broadcast();
				}
			}
			else{
				clients.put(nick_name,new ArrayList<String>(Arrays.asList(clnt_ip,clnt_port,clnt_status)));
				// broadcast to every user
				try{
					send("RegACK", clnt_ip, Integer.valueOf(clnt_port));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
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
				// System.out.println("off line msg table updated!");
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

		// System.out.println("after in register()");
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
