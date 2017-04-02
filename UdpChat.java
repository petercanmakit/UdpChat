// message content cannot contain "#"
import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

//***************************     MAIN      ************************************//
public class UdpChat{
	public static void main(String[] args) throws Exception {
      // UdpChat <mode> <command-line arguments>
	  if(args.length<2){
		  System.out.println("Useage: UdpChat <mode> <command-line arguments>\nFor server: Useage: UdpChat -s <port>\nFor client: UdpChat -c <nick-name> <server-ip> <server-port> <client-port> \n");
		  System.exit(1);
	  }
	  String mode = args[0];
	  if(mode.equals("-s")){	// server mode
		  if(args.length!=2){
			  System.out.println("Useage: UdpChat -s <port>\n");
			  System.exit(1);
		  }
		  else{
			  int port = Integer.valueOf(args[1]);
			  System.out.println("Server port is: "+port);
			  // start server with port
			  UdpChatServer server = new UdpChatServer(port);
			  // server created
			  int mutex = 0;
			  server.printInfo();
			  //server.send("Hello!","127.0.0.1",8081);

			  Thread t_serverReceiver = new Thread(new serverReceiver(server, mutex));
			  Thread t_serverHandler = new Thread(new serverHandler(server, mutex));

			  t_serverReceiver.start();
			  t_serverHandler.start();

			  t_serverReceiver.join();
			  t_serverHandler.join();

			  server.destroy();
		  }
	  }
	  else if(mode.equals("-c")){	//client mode
		  if(args.length!=5){
			  System.out.println("Useage: UdpChat -c <nick-name> <server-ip> <server-port> <client-port> \n");
			  System.exit(1);
		  }
		  else{
			  String nick_name = args[1];
			  String server_ip = args[2];
			  int server_port = Integer.valueOf(args[3]);
			  int client_port = Integer.valueOf(args[4]);

			  // start client
			  UdpChatClient client = new UdpChatClient(client_port,server_ip,server_port,nick_name);
			  // System.out.println("check client ds status:"+String.valueOf(client.ds.getInetAddress())+client.ds.getPort());


			  // System.out.println(client.recv());

			  int mutex = 1;
			  Thread t_sender = new Thread(new sender(client,mutex));
			  Thread t_receiver = new Thread(new receiver(client,mutex));
			  Thread t_printer = new Thread(new printer(client,mutex));
			  t_sender.start();
			  t_receiver.start();
			  t_printer.start();
			  // send register message to server
			  client.Register();
			  System.out.print(">>> ");

			  t_sender.join();
			  t_receiver.join();
			  t_printer.join();
			  client.destroy();
		  }
	  }

    }
}
