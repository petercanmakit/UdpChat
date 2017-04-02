import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/*
 * ServerReceiver thread keeps receiving from socket and put msg into messageQ
 * Synchronized with messageQ, notifies other waiting thread on messageQ
 */

public class ServerReceiver implements Runnable {
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
