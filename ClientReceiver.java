import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
public class ClientReceiver implements Runnable {	// keeps receiving from socket and put msg into messageQ
										// synchronized
	UdpChatClient client;
	public ClientReceiver(UdpChatClient cc) {
		this.client = cc;
	}

	@Override
	public void run() {
		while(true) {
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
