import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class TCPClient {
	private String httpMethod = null;
	private String restARGS = null;
	private String hostName;

	private URL URL = null;
	private int port = 80;

	private List<String> objectToDownload = null;

	private Socket clientSocket;

	private PrintWriter outputStream;
	private BufferedReader responseReader;
	private BufferedOutputStream dataOutputStream;

	private String newFileContent = null;

	public void run() throws Exception {

		String path = URL.getPath();

		if (httpMethod.equals("POST") || httpMethod.equals("PUT")) {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			System.out.println("\n\n File name on server: ");
			path += br.readLine();

			System.out.println("\n\n File content: ");
			newFileContent = br.readLine();
		}

		System.out.println("Client started on port: " + port);
		System.out.println("Client request host " + this.hostName);
		System.out.println("Client request method " + this.httpMethod);

		try {
			clientSocket = new Socket(this.hostName, this.port);

			responseReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			outputStream = new PrintWriter(clientSocket.getOutputStream(), true);
			dataOutputStream = new BufferedOutputStream(clientSocket.getOutputStream());

			if (path == "")
				path = "/";

			switch (httpMethod) {
			case "HEAD":
				fireHeadRequest(path);
				readHeadRequest();
				break;
			case "GET":
				fireGetRequest(path);
				readGetResponse();
				break;
			case "POST":
				firePostRequest(path);
				readPostResponse();
				break;
			case "PUT":
				firePutRequest(path);
				readPutRequest();
				break;
			default:
				System.out.println("HTTP Method not implemented");
				break;

			}

			responseReader.close();
			outputStream.close();
			dataOutputStream.close();

			clientSocket.close();
			System.out.println("Client closed!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readHeadRequest() throws IOException {
		System.out.println("\n SERVER HEAD RESPONSE ---------------");

		String line;

		// Read header data
		System.out.println(responseReader.readLine());
		while ((line = responseReader.readLine()) != null && line.length() > 0) {
			System.out.println("HTTP HEAD " + line);
		}

	}

	private void readPutRequest() throws NumberFormatException, IOException {
		System.out.println("\n SERVER PUT RESPONSE ---------------");

		String line;
		String contentLenghtString = "Content-Length: ";
		int contentLength = -1;

		// Read header data
		System.out.println(responseReader.readLine());
		while ((line = responseReader.readLine()) != null && line.length() > 0) {
			if (line.contains(contentLenghtString))
				contentLength = Integer.parseInt(line.substring(contentLenghtString.length()));
		}

		// Non chunked PUT response
		char[] buffer = new char[contentLength];
		int amountRead = responseReader.read(buffer, 0, contentLength);
		if (amountRead == contentLength) {
			System.out.println("PUT RESPONSE BODY = \n" + String.valueOf(buffer));
		}

	}

	private void readPostResponse() throws NumberFormatException, IOException {
		System.out.println("\nSERVER POST RESPONSE ---------------");

		String line;
		String contentLenghtString = "Content-Length: ";
		int contentLength = -1;

		// Read header data
		System.out.println(responseReader.readLine());
		while ((line = responseReader.readLine()) != null && line.length() > 0) {
			if (line.contains(contentLenghtString))
				contentLength = Integer.parseInt(line.substring(contentLenghtString.length()));
		}

		// Non chunked POST response
		char[] buffer = new char[contentLength];
		int amountRead = responseReader.read(buffer, 0, contentLength);
		if (amountRead == contentLength) {
			System.out.println("POST RESPONSE BODY = \n" + String.valueOf(buffer));
		}
	}

	private void readGetResponse() throws Exception {
		System.out.println("\nSERVER GET RESPONSE ---------------");

		String line;
		String contentLenghtString = "Content-Length: ";
		String transferEncoding = "Transfer-Encoding: ";
		boolean chunkSet = false;
		int contentLength = -1;
		char[] buffer;

		// Read header data
		while ((line = responseReader.readLine()) != null && line.length() > 0) {
			if (line.contains(contentLenghtString))
				contentLength = Integer.parseInt(line.substring(contentLenghtString.length()));

			if (line.contains(transferEncoding))
				chunkSet = line.substring(transferEncoding.length()).equals("chunked");
		}

		File file = new File(new File("clientFiles/"), "html.tmp");
		file.createNewFile();

		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("");
		fileWriter.close();
		fileWriter = new FileWriter(file, true);

		if (chunkSet) {
			int chunkLengthDec = 0;

			do {
				chunkLengthDec = Integer.parseInt(responseReader.readLine(), 16);

				if (chunkLengthDec > 0) {
					buffer = new char[chunkLengthDec];
					int offset = 0;

					while (offset < chunkLengthDec) {
						int amountRead = responseReader.read(buffer, offset, chunkLengthDec - offset);

						if (amountRead < 0)
							break;

						fileWriter.write(buffer);

						offset += amountRead;
					}
					responseReader.read();
					responseReader.read();
				}
			} while (chunkLengthDec > 0);

			fileWriter.flush();
			fileWriter.close();
		}

		else {
			// Non chunked GET response
			buffer = new char[contentLength];
			int amountRead = responseReader.read(buffer, 0, contentLength);

			if (amountRead == contentLength) {
				System.out.println("GET RESULT = " + String.valueOf(buffer));
				fileWriter.write(buffer);
				fileWriter.write("\n");
			}
			fileWriter.flush();
			fileWriter.close();
		}

		parseHtml();
		download();

	}

	private void firePutRequest(String path) throws IOException {
		outputStream.println("PUT " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println("Content-Length: " + newFileContent.length());
		outputStream.println();
		outputStream.flush();

		dataOutputStream.write(newFileContent.getBytes(Charset.forName("UTF-8")), 0, newFileContent.length());
		dataOutputStream.flush();
	}

	private void firePostRequest(String path) throws IOException {
		outputStream.println("POST " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println("Content-Length: " + newFileContent.length());
		outputStream.println();
		outputStream.flush();

		dataOutputStream.write(newFileContent.getBytes(Charset.forName("UTF-8")), 0, newFileContent.length());
		dataOutputStream.flush();
	}

	private void fireGetRequest(String path) {
		outputStream.println("GET " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println();
		outputStream.flush();
	}

	private void fireHeadRequest(String path) {
		outputStream.println("HEAD " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println();
		outputStream.flush();
	}

	public void parseParam(String[] args) throws Exception {
		if (args.length == 0)
			System.out.println("There are no arguments passed.");
		this.httpMethod = args[0];
		try {

//			if(httpMethods.contains(HTTPMETHOD)) throw new IllegalArgumentException("method not supported");
//			System.out.println("method " + HTTPMETHOD);
			String myUrl = args[1];
			if (!myUrl.contains("http://") && !myUrl.contains("https://"))
				myUrl = "http://" + myUrl;
			this.URL = new URL(myUrl);

		} catch (Exception e) {
			e.getStackTrace();
		}

		this.hostName = URL.getHost();
		this.port = Integer.parseInt(args[2]);

		for (int i = 3; i < args.length; i++) {
			restARGS = args[i];
			System.out.println("resting args " + args[i]);
		}
	}

	public void download() throws Exception {

		Image image = null;

		try {
			for (String obj : objectToDownload) {
				URL url = new URL(obj);

				System.out.println(url);
					

//				requestWriter.println(HTTPMETHOD + " " + obj + " HTTP/1.1");
//				requestWriter.println("HOST:" + HOSTNAME);
//				requestWriter.println();
				
				
				String[] parts = obj.toString().split("/");
				String fileName = parts[parts.length - 1];
				DataInputStream in = new DataInputStream(clientSocket.getInputStream());
			    DataOutputStream bw = new DataOutputStream(new DataOutputStream(clientSocket.getOutputStream()));
			    
			    
			    System.out.println("xx");
			    
			    String cmd =  "GET /solar.jpg HTTP/1.1\r\n";
			    bw.write(cmd.getBytes());
			    System.out.println("xx");
			    cmd = "Host: localhost:8081\r\n\r\n";
			    bw.writeBytes(cmd);
			    bw.flush();
			    
			    
//			    bw.writeBytes("GET /"+fileName+" HTTP/1.1\n");
//			    bw.writeBytes("Host: "+HOSTNAME+"\n\n");
			    System.out.println("xx");
				OutputStream dos = new FileOutputStream("testtttt.jpg");
				int count;
				byte[] buffer = new byte[2048];
				boolean eohFound = false;
				System.out.println("xx");
				while ((count = in.read(buffer)) != -1)
				{
				    if(!eohFound){
				        String string = new String(buffer, 0, count);
				        int indexOfEOH = string.indexOf("\r\n\r\n");
				        if(indexOfEOH != -1) {
				            count = count-indexOfEOH-4;
				            buffer = string.substring(indexOfEOH+4).getBytes();
				            eohFound = true;
				        } else {
				            count = 0;
				        }
				    }
				  dos.write(buffer, 0, count);
				  dos.flush();
				}
				in.close();
				dos.close();
//
//				System.out.println("SERVER RESPONSE ---DOWNLOADING-IMAGES---");
//				String line;
//				OutputStream dos;
//				String[] parts = obj.toString().split("/");
//				String fileName = parts[parts.length - 1];
//				dos = new FileOutputStream(fileName);
//				int count;
//				byte[] buffer = new byte[2048];
//				while ((count = response.read(buffer)) != -1) {
//					dos.write(buffer, 0, count);
//					dos.flush();
//				}
//				dos.close();

//			    
//			    File outputfile = new File(fileName);
//
//
//			    ImageIO.write((RenderedImage)  image, "jpg", outputfile);
			}
			System.out.println("Images successfully stored locally.");
		} catch (IOException e) {
			System.err.println("Failed saving the images");
			e.printStackTrace();
		}
	};

	public void parseHtml() throws IOException {
		objectToDownload = new ArrayList<>();
		File input = new File("clientFiles/html.tmp");

		String changedHostname = URL.toString();
		if (URL.toString().contains("http://localhost/")) {
			changedHostname = "http://localhost:" + this.port;

		}

		Document doc = Jsoup.parse(input, "UTF-8", changedHostname);
		Elements img = doc.getElementsByTag("img");
		
		for (Element el : img) {
			String src = el.absUrl("src");
//			System.out.println("Image Found!");
//			System.out.println("src attribute is : "+src);

//			System.out.println(src.getClass());
			objectToDownload.add(src.toString());

		}

	}
}