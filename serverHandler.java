import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ServerHandler implements Runnable {		// keeps reading from messageQ

	UdpChatServer server;
	public ServerHandler(UdpChatServer cc){
		this.server = cc;
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
				// System.out.println("I am in while!");
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
