import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TCPClient {
//	private String HTTPMETHOD = null;
//	private String URI = null;
//	private static int PORT = 80;
//	private String restARGS = null;
//	
//	
//	public void parseArguments(String argv[]) {
//		if (argv.length == 0) System.out.println("There are no arguments passed.");
//		try {
//			HTTPMETHOD = argv[0];
//			System.out.println("method " + HTTPMETHOD);
//			URI = argv[1];
//			System.out.println("uri " + URI);
//			PORT = Integer.parseInt(argv[2]);
//			System.out.println("port " + PORT);
//			
//			for (int i=3; i < argv.length; i++) {
//				restARGS = argv[i];
//				System.out.println("resting args " + argv[i]);
//			}
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
//		
//	}
	

	public static void main(String[] args) throws UnknownHostException, IOException {
		if (args.length < 1) return;

		URL url;
		String hostname;
		try {
			url = new URL(args[0]);
			hostname = url.getHost();
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return;
		}
	

		//String hostname = url.getHost();
		int port = 80;
		
		System.out.println("Client started on port: "  + port);
		
		
		
		

		//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		Socket clientSocket = new Socket(hostname, port);
		OutputStream out = clientSocket.getOutputStream();

		PrintWriter requestWriter = new PrintWriter(out, true);
		
		InputStream response = clientSocket.getInputStream();
		BufferedReader responseReader = new BufferedReader(new InputStreamReader(response));
		

		
//		String sentence = inFromUser.readLine();
		requestWriter.println("GET www.tcpipguide.com/images/readdown.png HTTP/1.1");
		requestWriter.println();
		

//		outToServer.writeBytes(sentence + '\n'); 
		
		System.out.println("SERVER RESPONSE ---------------");
//		while(inFromServer.readLine() != null) {
//			System.out.println(inFromServer.readLine());
//		}
		
		
//		String responseLine;
//        while ((responseLine = responseReader.readLine()) != null) {
//            System.out.println("Server: " + responseLine);
//            if (responseLine.indexOf("Ok") != -1) {
//              break;
//            }
//        }
		String line;
		while ((line = responseReader.readLine()) != null) {
			System.out.println(line);
		}

		System.out.println("Client closed!");
		clientSocket.close();
	}
}