import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/*
 * ServerHandler keeps reading from messageQ
 * Handles two kinds of message: pingACK and register message
 */

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

				String msg = server.messageQ.poll();
				if(msg.equals("pingACK")){
					// still alive send back offline mesg request err
					server.gotPingACK();
				}
				else{
					// ordinary register message
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
