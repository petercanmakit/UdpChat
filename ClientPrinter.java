import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/*
 * ClientPrinter thread keeps reading from message queue.
 * if ordinary message, print it out;
 * if any kind ACK message, change the according flag
 * in order to let the specific waiting thread go to next step
 */

public class ClientPrinter implements Runnable {
	UdpChatClient client;
	public ClientPrinter(UdpChatClient cc){
		this.client = cc;
	}

	@Override
	public void run(){
		// System.out.println("printer starts working!");
		while(true){
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
				// System.out.println("printer keeps working! while loop");
				// System.out.println("Q empty? : "+ client.messageQ.isEmpty());
				String msg = client.messageQ.poll();

				if(msg.equals("pingDead")){
					try{
						client.send("pingACK", client.server_ip, client.server_port);
						// System.out.println("I send pingACK to server!");
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
				else if(msg.equals("offACK") || msg.equals("RegACK")) {
					// offACK or RegACK both from server for reg and dereg
					// System.out.println("Got an offACK");
					try{
						client.gotOffACK();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else if(msg.equals("offMsgACK")) {
					client.gotOffACK();
					System.out.println("[Messages received by the server and saved]");
				}
				else if(msg.startsWith("offMsgERR")) {
					String err_usrname = msg.split("#")[1];
					System.out.println("[Client "+err_usrname+" exists!!]");
					System.out.print(">>> ");
				}
				else if(msg.startsWith("offMsgSending#")) {
					String offmsg = msg.split("#")[1];
					System.out.println(offmsg);
					System.out.print(">>> ");
				}
				else { // ordinary message
					String sender_name = msg.split(":")[0];
					if(sender_name.equals(client.nick_name))
						System.out.print(">>> ");
					System.out.println(msg);
					if(!sender_name.equals(client.nick_name))
						System.out.print(">>> ");
					String back_name = msg.split(":\\s+")[0];
					String back_ip = client.clients.get(back_name).get(0);
					int back_port = Integer.valueOf(client.clients.get(back_name).get(1));
					try {
						client.send("ACK", back_ip, back_port);
						// System.out.println("in printer: ACK sent to back_ip: "+back_ip+", back_port"+back_port);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
