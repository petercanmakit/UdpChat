import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/*
 * ClientSender thread reads from user input
 * sends message to peer user or server
 * after sending, wait for ACK accordingly
 */

public class ClientSender implements Runnable { // including sending deRegisteration to server
	UdpChatClient client;
	public ClientSender(UdpChatClient cc){
		this.client = cc;
	}

	@Override
	public void run(){
		while(true){
			Scanner sc = new Scanner(System.in);
			String line = sc.nextLine();
			// System.out.println(line);
			String[] usr_in = line.split("\\s+",3);

			// System.out.println(usr_in[0]);
			if(usr_in[0].equals("send")){
				if(usr_in.length<3){
					System.out.println(">>> Usage: send <name> <message>");
				}
				else{	// insert request is correct
					String name = usr_in[1]; 	// intend receiver name

					if(client.clients.containsKey(name)){
						String message = usr_in[2];	// intend sending msg

						//System.out.println(client.clients.toString());
						// System.out.println("intedn status is "+client.clients.get(name).get(2));
						String intend_status = client.clients.get(name).get(2);
						// System.out.println("intedn status equals on? : "+ intend_status.equals("on"));
						// System.out.println(intend_status+intend_status+intend_status);
						if(client.clients.get(name).get(2).equals("on")){
							try{
								client.send_P2P(client.nick_name+":  "+message, name);
							}
							catch(Exception ex){
								ex.printStackTrace();
							}
						}
						else if(client.clients.get(name).get(2).equals("off")){
							// the intend receiver is offline send it to server as offline msg
							client.backOffACK();
							for(int i = 0; i<5 ;i++){

								try{
								client.send("off_m#"+ client.nick_name + "#" + name + "#" +
											client.nick_name + ":  " + message,
											client.server_ip, client.server_port);
								}
								catch (Exception e) {
									System.out.println(e);
									System.exit(1);
								}
								try{
									Thread.sleep(500);
								}
								catch (Exception e) {
									e.printStackTrace();
								}
								if(client.offACK_status) break;

							}
							if(client.offACK_status){
								client.backOffACK();
							}
							else{
								System.out.print(">>> [Server not responding]\n>>> [Exiting]\n");
								System.exit(0);
							}

						}

					}

					else{
						// table not contain this user
						System.out.print(">>> [Client " + name + " does not exist. Check the client table below: ]\n>>>");
						client.printClinetsTable();
					}
				}

			}
			else if(usr_in[0].equals("dereg")){
				if(usr_in.length==1){
					client.deRegister();
				}
				else if(!usr_in[1].equals(client.nick_name)){
					System.out.println(">>> [You cannot dereg user other than youself...]");
				}
				else{
					client.deRegister();
				}
			}
			else if(usr_in[0].equals("reg")){
				// System.out.println("in reg section!");
				if(usr_in.length==1 || usr_in[1].equals(client.nick_name)){
					client.Register();
				}
				else if(usr_in.length>1){
					System.out.println(">>> [You cannot register user other than youself...]");
				}
			}
			else{
				System.out.println(">>> [Usage: You can send/reg/dereg]");
			}
			System.out.print(">>> ");
		}
	}
}
