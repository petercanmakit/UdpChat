import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/*
 * ClientReceiver thread keeps receiving from socket and put msg into message queue
 * Synchronized with messageQ, it notifies other waiting thread
 */

public class ClientReceiver implements Runnable {
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
