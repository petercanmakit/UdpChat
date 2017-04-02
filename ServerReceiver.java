import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ServerReceiver implements Runnable {	// keeps receiving from socket and put msg into messageQ
										// synchronized
	UdpChatServer server;
	public ServerReceiver(UdpChatServer cc){
		this.server = cc;
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
