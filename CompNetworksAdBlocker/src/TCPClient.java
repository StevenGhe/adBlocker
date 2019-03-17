import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;

class TCPClient {
	private String HTTPMETHOD = null;
	private URL URL = null;
	private int PORT = 80;
	private String restARGS = null;
	private String HOSTNAME;
	private List<String> httpMethods = Arrays.asList("GET", "HEAD", "POST", "PUT");
	private List<String> objectTodDownload;
	private Socket clientSocket;
	private PrintWriter requestWriter;
	private BufferedReader responseReader;
	private InputStream response;

	public void parseParam(String[] args) throws Exception {
		if (args.length == 0)
			System.out.println("There are no arguments passed.");
		this.HTTPMETHOD = args[0];
		try {

//			if(httpMethods.contains(HTTPMETHOD)) throw new IllegalArgumentException("method not supported");
//			System.out.println("method " + HTTPMETHOD);
			String myUrl = args[1];
			if (!myUrl.contains("http://") && !myUrl.contains("https://"))
				myUrl = "http://" + myUrl;
			this.URL = new URL(myUrl);

		} catch (Exception e) {
			throw new Exception("help");
		}

		this.HOSTNAME = URL.getHost();
		this.PORT = Integer.parseInt(args[2]);

		for (int i = 3; i < args.length; i++) {
			restARGS = args[i];
			System.out.println("resting args " + args[i]);
		}
	}

	public void download() throws Exception {

		Image image = null;

		try {
			for (String obj : objectTodDownload) {
				URL url = new URL(obj);

				System.out.println(url);

				System.out.println(HTTPMETHOD + " " + obj + " HTTP/1.1");
				requestWriter.println(HTTPMETHOD + " " + obj + " HTTP/1.1");
				requestWriter.println("HOST:" + HOSTNAME);
				requestWriter.println();

				System.out.println("SERVER RESPONSE ---DOWNLOADING-IMAGES---");
				String line;
				OutputStream dos;
				String[] parts = obj.toString().split("/");
				String fileName = parts[parts.length - 1];
				dos = new FileOutputStream(fileName);
				int count;
				byte[] buffer = new byte[2048];
				while ((count = response.read(buffer)) != -1) {
					dos.write(buffer, 0, count);
					dos.flush();
				}
				dos.close();

//			    
//			    File outputfile = new File(fileName);
//
//
//			    ImageIO.write((RenderedImage)  image, "jpg", outputfile);
			}
			System.out.println("Images successfully stored locally.");
		} catch (IOException e) {
			System.err.println("Failed saving the images");
		}
	};

	public void run() throws Exception {

		System.out.println("Client started on port: " + PORT);
		System.out.println("---------------------------------");
		System.out.println("host " + this.HOSTNAME);
		System.out.println("method " + this.HTTPMETHOD);

		PrintWriter writer = null;

		try {
			clientSocket = new Socket(this.HOSTNAME, this.PORT);
			OutputStream out = clientSocket.getOutputStream();

			requestWriter = new PrintWriter(out, true);

			response = clientSocket.getInputStream();
			InputStreamReader responseStream = new InputStreamReader(response);
			responseReader = new BufferedReader(responseStream);
			String path = URL.getPath();
			if (path == "") {
				System.err.println("No path specified -> root ");
				path = "/";
			}
			requestWriter.println(HTTPMETHOD + " " + path + " HTTP/1.1");
			requestWriter.println("Host: " + HOSTNAME);
			System.out.println("Host: " + HOSTNAME);
			requestWriter.println();
			requestWriter.flush();

			System.out.println("SERVER RESPONSE ---------------");

//			writer = new PrintWriter("html.tmp", "UTF-8");

			String line;
			String contentLenghtString = "Content-Length: ";
			String transferEncoding = "Transfer-Encoding: ";
			boolean chunkSet = false;
			int contentLength = -1;

			while ((line = responseReader.readLine()) != null && line.length() > 0) { // whileloop header
//				System.out.println(line);
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

			char[] buffer;

			if (chunkSet) {
				int chunkLength = 0;

				do {
					chunkLength = Integer.parseInt(responseReader.readLine(), 16);

					if (chunkLength > 0) {
						System.out.println("ChunksizeInDec= " + chunkLength);

						buffer = new char[chunkLength];
						int offset = 0;

						while (offset < chunkLength) {
							System.out.println("BYTESREAD: " + offset);
							int amountRead = responseReader.read(buffer, offset, chunkLength - offset);

							if (amountRead < 0)
								break;

							System.err.println("AmtRead= " + amountRead);

							fileWriter.write(buffer);
							System.out.println("GET CHUNKRESULT = " + String.valueOf(buffer));

							offset += amountRead;
						}
						responseReader.read();
						responseReader.read();
					}
					System.out.println("chunk read@@@@@@@@@@@@@@@@@@@\n");
				} while (chunkLength > 0);

				fileWriter.flush();
				fileWriter.close();
			}

			else {
				System.out.println(contentLength);
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

			clientSocket.close();
			System.out.println("Client closed!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void parseHtml() throws IOException {
		objectTodDownload = new ArrayList<>();
		File input = new File("html.tmp");
		String changedHostname = URL.toString();
		if (URL.toString().contains("http://localhost/")) {
			changedHostname = "http://localhost:" + this.PORT;

		}
//		Document doc = Jsoup.parse(input, "UTF-8", changedHostname);
//		Elements img = doc.getElementsByTag("img");
//		for (Element el : img) {
//			String src = el.absUrl("src");
////			System.out.println("Image Found!");
////			System.out.println("src attribute is : "+src);
//
////			System.out.println(src.getClass());
//			objectTodDownload.add(src.toString());

//		}
//		;

	}
}