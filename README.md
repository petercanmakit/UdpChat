Jiajun Zhang, UNI: jz2793

## UdpChat: Command line instructions

### 1. Compile:

	make

After compiling, there is a simple bash script which allows run the output file without "java" command.

### 2. Start Server:

	./UdpChat -s <port>
e.g:

	./UdpChat -s 8000
You should see server information like:

	Server created:
	IpAddr:160.39.140.239
	Port: 8000

### 3. Start Client:

	./UdpChat -c <nick-name> <server-ip> <server-port> <client-port>
e.g:

	./UdpChat -c peter 160.39.140.239 8000 8010

You should see client information like:

	>>> [Welcome. You are registered.]		
	>>> [Client table updated.]
	>>> [ peter: on ]

Now you can use send/deregister/reregister command.


#### 3.1 Registration/de-registration	

The first time you log with your nickname and port, you are registered immediately.		
Now you can de-regiser by type:	
	>>> dereg <nick_name>
	
or simply:	
	>>> dereg

And then you can regiser by:	
	>>> reg <nick_name>	
or:	
	>>> reg	

The example:
	>>> dereg
	>>> [You are Offline. Bye.]
	>>> reg peter
	>>> [Client table updated.]
	>>> [ zjj: on ]	[ peter: on ]

On other client side:
	>>> [Client table updated.]
	>>> [ zjj: on ]	[ peter: on ]
	>>> [Client table updated.]
	>>> [ zjj: on ]	[ peter: off ]
	>>> [Client table updated.]
	>>> [ zjj: on ]	[ peter: on ]

Note: If you ctrl+C and use command like showed before, you can log back.

If you dereg but the server is down, it will show:
	>>> dereg
	>>> [Server not responding]
	>>> [Exiting]

#### 3.2 Chatting

	>>> send <receriver_name> <message>
e.g.:	
	>>> send zjj hi
	>>> [Message received by zjj.]

At receiver side:
	>>> peter:  hi

If the receiver is offline, the sender will send the message to server. If the receiver just ctrl+C before dereg the sender will try 5 times in total if no ACK received then send it to server. The server will ping this "dead" user, if not pingACK back, it will change it's status to offline.
	>>> send zjj Hello
	>>> [Messages received by the server and saved]

#### 3.3 Off-line Chat	
peter:
	>> [ zjj: off ]	[ peter: on ]
	>>> send zjj Are you there?
	>>> [Messages received by the server and saved]

And when peter logs in, he will receive all the off-line messages from the server.
	>>> reg
	>>> [Client table updated.]
	>>> [ zjj: on ]	[ peter: on ]	[ haha: on ]
	>>> [You have messages]
	>>> peter:  01/03/17 17:59:33  Are you there?
	>>> haha:  01/03/17 18:00:35  hi
	>>> haha:  01/03/17 18:00:40  hi2222

If log off and log on again, these messages will not appear since they are deleted at server side.
	>>> dereg
	>>> [You are Offline. Bye.]
	>>> reg
	>>> [Client table updated.]
	>>> [ zjj: on ]	[ peter: on ]	[ haha: on ]

When one quited the program before dereg, the status remains "on". If someone sends message to it, sender will not receive ACK so send to server. After testing, the server will notice it actually died, so changes its status to "off".
For example, peter ctrl+C before dereg. And then zjj sends message to him.
	>>> [ zjj: on ]	[ peter: on ]	[ haha: on ]
	>>> send peter hihihi
	>>> [No ACK from peter, message sent to server.]
	>>> [Client table updated.]
	>>> [ zjj: on ]	[ peter: off ]	[ haha: on ]
	>>> [Messages received by the server and saved]

Server ping to peter, but receives no pingACK. So the status of peter is set to "off", and the server saved the former message as an offline message. And tells zjj that it saved it for him.

#### 3.4 Other features:

If one user logs in using another different PC (with different ip and/or port), it will just update the table so that everyone knows the updated contact information. It seams the nikc-name is the key to login.	
Unfortunately, you cannot send any messages that contain "#"... It's a drawback.
