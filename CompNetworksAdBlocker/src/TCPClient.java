import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TCPClient {
	private String HTTPMETHOD = null;
	private URL URL = null;
	private int PORT = 80;
	private String restARGS = null;
	private String HOSTNAME;
	private List<String> httpMethods = Arrays.asList("GET", "HEAD", "POST", "PUT");
	
	
	public void parseParam(String[] args) throws Exception {
		if (args.length == 0) System.out.println("There are no arguments passed.");
		this.HTTPMETHOD = args[0];
		try {
			
//			if(httpMethods.contains(HTTPMETHOD)) throw new IllegalArgumentException("method not supported");
//			System.out.println("method " + HTTPMETHOD);
			String myUrl = args[1];
			if(!myUrl.contains("http://") || !myUrl.contains("https://")) myUrl = "http://"+myUrl;
			this.URL = new URL(myUrl);
			
		} catch (Exception e) {
			throw new Exception("help");
		}
		
		this.HOSTNAME = URL.getHost();
		System.out.println("url" + URL);
		this.PORT = Integer.parseInt(args[2]);
		System.out.println("port " + PORT);
		
		for (int i=3; i < args.length; i++) {
			restARGS = args[i];
			System.out.println("resting args " + args[i]);
		}
	}
	
	
	

	public void run() throws UnknownHostException, IOException {

		System.out.println("Client started on port: "  + PORT);
		System.out.println(this.HOSTNAME);
		System.out.println(this.PORT);
		
		Socket clientSocket = new Socket(this.HOSTNAME, this.PORT);
		OutputStream out = clientSocket.getOutputStream();

		PrintWriter requestWriter = new PrintWriter(out, true);
		
		InputStream response = clientSocket.getInputStream();
		BufferedReader responseReader = new BufferedReader(new InputStreamReader(response));
		

		requestWriter.println( HTTPMETHOD+" "+ URL +" HTTP/1.1");
		requestWriter.println();

		
		System.out.println("SERVER RESPONSE ---------------");

		String line;
		while ((line = responseReader.readLine()) != null) {
			System.out.println(line);
		}

		System.out.println("Client closed!");
		clientSocket.close();
	}
}