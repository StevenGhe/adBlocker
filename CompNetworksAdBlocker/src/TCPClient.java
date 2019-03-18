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
	private DataInputStream dataResponseReader;
	private BufferedOutputStream dataOutputStream;

	private String newFileContent = null;

	public void run() throws Exception {

		String path = URL.getPath();

		if (path == "")
			path = "/";

		// If post or put, ask input before sending request
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
			dataResponseReader = new DataInputStream(clientSocket.getInputStream());

			outputStream = new PrintWriter(clientSocket.getOutputStream(), true);
			dataOutputStream = new BufferedOutputStream(clientSocket.getOutputStream());

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

			clientSocket.close();
			System.out.println("Client closed!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fireHeadRequest(String path) {
		outputStream.println("HEAD " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println();
		outputStream.flush();
	}

	private void readHeadRequest() throws IOException {
		System.out.println("\n SERVER HEAD RESPONSE ---------------");

		String line;

		// Print header data
		System.out.println(responseReader.readLine());
		while ((line = responseReader.readLine()) != null && line.length() > 0) {
			System.out.println("HTTP HEAD " + line);
		}

	}

	private void fireGetRequest(String path) {
		outputStream.println("GET " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println();
		outputStream.flush();
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

		// Create file to store data
		File file = new File("clientFiles/tmp.html");
		file.createNewFile();

		// If existed: clean file
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write("");
		fileWriter.close();
		fileWriter = new FileWriter(file, true);

		// If Transfer encoding was set && it was set to chunked
		if (chunkSet) {
			int chunkLengthDec = 0;

			do {
				// Read first hex line (=chunk size)
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
                String bufferString = new String(buffer);

            if(bufferString.contains("ad")) {
                    buffer = removeAds(bufferString);
                }
                System.out.println("GET RESULT = " + String.valueOf(buffer));


                fileWriter.write(buffer);
//                System.err.println(buffer);
                fileWriter.write("\n");
            }
            fileWriter.flush();
            fileWriter.close();
        }
	}
	
	private char[] removeAds(String buffer) {

        String oldBuffer = buffer;
        Document doc = Jsoup.parse(buffer);

        Elements img = doc.getElementsByTag("img");


        for (Element el : img) {
            System.out.println(el);


            String htmlLine = el.toString();

            int height = 0;
            int width = 0;
            if(htmlLine.contains("ad")) {
                System.err.println("yesss finally");
                height = Integer.parseInt(el.attr("height"));
                width = Integer.parseInt(el.attr("width"));
                String replacementString = "<div class="adReplacementDiv" style="width:"+width+"px; height:"+height+"px;"></div>";
                oldBuffer = oldBuffer.replace(htmlLine, replacementString);
            }
        }
        return oldBuffer.toCharArray();
    }

	private void firePostRequest(String path) throws IOException {
		// Specify contentlength
		outputStream.println("POST " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println("Content-Length: " + newFileContent.length());
		outputStream.println();
		outputStream.flush();

		// use data output stream to stream bytes instead of strings
		dataOutputStream.write(newFileContent.getBytes(Charset.forName("UTF-8")), 0, newFileContent.length());
		dataOutputStream.flush();
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
			// Post body is data of the edited file
			System.out.println("POST RESPONSE BODY = \n" + String.valueOf(buffer));
		}
	}

	private void firePutRequest(String path) throws IOException {
		// Specify contentlength
		outputStream.println("PUT " + path + " HTTP/1.1");
		outputStream.println("Host: " + hostName);
		outputStream.println("Content-Length: " + newFileContent.length());
		outputStream.println();
		outputStream.flush();

		// use data output stream to stream bytes instead of strings
		dataOutputStream.write(newFileContent.getBytes(Charset.forName("UTF-8")), 0, newFileContent.length());
		dataOutputStream.flush();
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
			// Put body is data of the edited file
			System.out.println("PUT RESPONSE BODY = \n" + String.valueOf(buffer));
		}

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

	public void download() {
		System.out.println("\n\n\n DOWNLOADING IMAGES -----------");
		try {
			//For each image that needs to be downloaded
			for (String imgFile : objectToDownload) {
				
				//Setup new connection with readers
				clientSocket = new Socket(this.hostName, this.port);

				String[] parts = imgFile.toString().split("/");
				String fileName = parts[parts.length - 1];

				DataOutputStream imageOutputStream = new DataOutputStream(clientSocket.getOutputStream());
				dataResponseReader = new DataInputStream(clientSocket.getInputStream());
				responseReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

				System.out.println("GET /" + fileName + " HTTP/1.1");
				imageOutputStream.writeBytes("GET " + fileName + " HTTP/1.1\r\n");
				imageOutputStream.writeBytes("Host: " + hostName + "\r\n\r\n");
				imageOutputStream.flush();

				String line;
				String contentLenghtString = "Content-Length: ";
				int contentLength = -1;

				// Get content length from header
				System.out.println(responseReader.readLine());
				while ((line = responseReader.readLine()) != null && line.length() > 0) {
					if (line.contains(contentLenghtString))
						contentLength = Integer.parseInt(line.substring(contentLenghtString.length()));
				}

				System.out.println("Response image Content-Lenght " + contentLength + "\n");

				//Binairy output stream to file
				OutputStream dos = new FileOutputStream("clientImg/" + fileName);

				byte[] buffer = new byte[contentLength];
				dataResponseReader.read(buffer, 0, contentLength);

				dos.write(buffer);
				dos.flush();
				dos.close();
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