import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.Console;

class TCPClient {
<<<<<<< HEAD
	private String HTTPMETHOD = null;
	private String URI = null;
	private String PORT = null;
	private String restARGS = null;
	
	
	public void parseArguments(String argv[]) {
		if (argv.length == 0) System.out.println("There are no arguments passed.");
		try {
			HTTPMETHOD = argv[0];
			System.out.println("method " + HTTPMETHOD);
			URI = argv[1];
			System.out.println("uri " + URI);
			PORT = argv[2];
			System.out.println("port " + PORT);
			
			for (int i=3; i < argv.length; i++) {
				restARGS = argv[i]
				System.out.println("resting args " + argv[i]);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	
	
	
	
		
		

		
		
		
//		 char[] caMainArg = null;
//	     String strMainArg = null;
//		
//		for(String arg: argv) {
//	        // Convert each String arg to char array
//	        caMainArg = arg.toCharArray();
//
//	        // Convert each char array to String
//	        strMainArg = new String(caMainArg);
//	    }
//	    System.out.print(strMainArg);
			
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		Socket clientSocket = new Socket("localhost", 6799);
=======
	public static void main(String argv[]) throws Exception {
		System.out.println("Client started!");
		Socket clientSocket = new Socket("localhost", TCPServer.PORT);

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
>>>>>>> e94318c08fc80f2cd5fb4572bd61fba6e85a107f
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		
		String sentence = inFromUser.readLine();
		outToServer.writeBytes(sentence + '\n');
		
		String modifiedSentence = inFromServer.readLine();
		System.out.println("FROM SERVER: " + modifiedSentence);
		clientSocket.close();
		
		System.out.println("Client closed!");
	}
}