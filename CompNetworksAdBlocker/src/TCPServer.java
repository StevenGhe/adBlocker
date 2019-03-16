import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class TCPServer implements Runnable {
	static final int PORT = 8081;
	private static int connectCounter = 0;
	private Socket socket;

	static final File WEB_ROOT = new File("webContent/");
	static final File FILE_ROOT = new File("saveFiles/");
	static final String DEFAULT_FILE = "index.html"; // 200

	static final String NOT_MODIFIED = "304.html";
	static final String BAD_REQUEST = "400.html";
	static final String METHOD_NOT_SUPPORTED = "405.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String SERVER_ERROR = "500.html";

	static final boolean verbose = true;

	public TCPServer(Socket port) {
		this.socket = port;
	}

	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);

//			if (verbose)
//				System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

			while (true) {
				TCPServer myServer = new TCPServer(serverConnect.accept());

//				if (verbose)
//					System.out.println(
//							"New connection! (" + new Date() + ") | Connection counter: " + ++connectCounter + "\n");

				Thread thread = new Thread(myServer);
				thread.start();
			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		BufferedReader inputStream = null;
		PrintWriter outputStream = null;
		BufferedOutputStream dataOutputStream = null;

		String fileRequested = null;
		String method = null;
		String httpVersion = null;

		try {
			inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outputStream = new PrintWriter(socket.getOutputStream());
			dataOutputStream = new BufferedOutputStream(socket.getOutputStream());

			//Reading first line
			String[] parsedFirstLine = inputStream.readLine().split(" ");
			method = parsedFirstLine[0].toUpperCase();
			fileRequested = parsedFirstLine[1].toLowerCase();
			httpVersion = parsedFirstLine[2].toUpperCase();

			String s = null, connection = null, host = null;
			int contentLength = 0;
			String hostString = "Host: ";
			String contentLenghtString = "Content-Length: ";
			String keepAlive = "Connection: ";
			
			//Reading header
			while ((s = inputStream.readLine()) != null && s.length() > 0) {
				if (s.contains(hostString))
					host = s.substring(hostString.length());

				if (s.contains(contentLenghtString))
					contentLength = Integer.parseInt(s.substring(contentLenghtString.length()));

				if (s.contains(keepAlive)) {
					connection = s.substring(keepAlive.length());
				}
				if (s.isEmpty()) {
					break;
				}
			}
			System.out.println("READ KEEP ALIVE: " + connection);
			System.out.println("READ HOST: " + host);
			System.out.println("READ Content length: " + contentLength);

			switch (method) {
			case "HEAD":
				methodHeadGet(outputStream, dataOutputStream, fileRequested, false);
				break;
			case "GET":
				methodHeadGet(outputStream, dataOutputStream, fileRequested, true);
				break;
			case "PUT":
				methodPUT(inputStream, outputStream, dataOutputStream, fileRequested, contentLength);
				break;
			case "POST":
				methodPOST(inputStream, outputStream, dataOutputStream, fileRequested, contentLength);
				break;
			default:
				methodNotSupported(outputStream, dataOutputStream, method);
				break;
			}

		} catch (FileNotFoundException e) {

			try {
				fileNotFound(outputStream, dataOutputStream, fileRequested);
			} catch (IOException e2) {
				System.err.println("Error with file not found exception : " + e2.getMessage());
			}

		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				inputStream.close();
				outputStream.close();
				dataOutputStream.close();
				socket.close();
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}

//			if (verbose) {
//				System.out.println("Connection closed.");
//				System.out.println("----------------------\n");
//			}
		}

	}

	private void methodPOST(BufferedReader inputStream, PrintWriter outputStream, BufferedOutputStream dataOutputStream,
			String fileRequested, int contentLength) throws NumberFormatException, IOException {
		File file = new File(FILE_ROOT, fileRequested);
		if (file.createNewFile()) {
			if (verbose)
				System.out.println("File not yet found. Creating new file!");
			this.methodPUT(inputStream, outputStream, dataOutputStream, fileRequested, contentLength);
		} else {

			if (verbose)
				System.out.println("Going to edit file " + file.getCanonicalFile());

			FileWriter fileWriter = new FileWriter(file, true);

			char[] buffer = new char[contentLength];
			int rsz = inputStream.read(buffer, 0, contentLength);
			if (rsz == contentLength) {
				fileWriter.write(buffer);
				fileWriter.write("\n");
			}

			fileWriter.flush();
			fileWriter.close();

			int newContentLength = (int) file.length();

			outputStream.println("HTTP/1.1 201 OK");
			outputStream.println("Date: " + new Date());
			outputStream.println("Server: Java HTTP Server");
			outputStream.println("Location: " + file.getCanonicalFile());
			outputStream.println("Content-type: " + getContentTypeOfFile(fileRequested));
			outputStream.println("Content-length: " + newContentLength);
			outputStream.println();
			outputStream.flush();
			
			byte[] data = readFileData(file, newContentLength);
			dataOutputStream.write(data, 0, newContentLength);
			dataOutputStream.write(0);
			dataOutputStream.flush();
		}
	}

	private void methodPUT(BufferedReader inputStream, PrintWriter outputStream, BufferedOutputStream dataOut,
			String fileRequested, int contentLength) throws IOException {
		File file = new File(FILE_ROOT, fileRequested);

		if (file.createNewFile()) {
			if (verbose)
				System.out.println("File " + file.getCanonicalPath() + " is created!\n\n");

			FileWriter fileWriter = new FileWriter(file);
			StringBuilder outputBuilder = new StringBuilder();

			char[] buffer = new char[contentLength];
			int amtOfCharsRead = inputStream.read(buffer, 0, contentLength);

			if (amtOfCharsRead == contentLength) {
				fileWriter.write(buffer);
				fileWriter.write("\n");
			}

			fileWriter.flush();
			fileWriter.close();

			outputStream.println("HTTP/1.1 201 CREATED");
			outputStream.println("Date: " + new Date());
			outputStream.println("Server: Java HTTP Server");
			outputStream.println("Content-type: " + getContentTypeOfFile(fileRequested));
			outputStream.println("Content-length: " + contentLength);
			outputStream.println();
			outputStream.flush();
			
			byte[] data = readFileData(file, contentLength);
			dataOut.write(data, 0, contentLength);
			dataOut.write(0);
			dataOut.flush();

		} else {
			// TODO: Do we need to return an error?
			System.out.println("File already exists.");
		}
	}

	private void methodHeadGet(PrintWriter out, BufferedOutputStream dataOut, String fileRequested,
			boolean isMethodGetRequest) throws IOException {
		if (fileRequested.equals("/"))
			fileRequested = DEFAULT_FILE;

		File file = new File(WEB_ROOT, fileRequested);
		String contentType = getContentTypeOfFile(fileRequested);
		int contentLength = (int) file.length();

		out.println("HTTP/1.1 200 OK");
		out.println("Date: " + new Date());
		out.println("Server: Java HTTP Server");
		out.println("Content-type: " + contentType);
		out.println("Content-length: " + contentLength);
		out.println();
		out.flush();

		if (isMethodGetRequest) {
			byte[] data = readFileData(file, contentLength);
			dataOut.write(data, 0, contentLength);
			dataOut.write(0);
			dataOut.flush();
		}

		if (verbose) {
			System.out.print(isMethodGetRequest ? "GET" : "HEAD");
			System.out.println(": File " + fileRequested + " of type " + contentType + " returned");
		}
	}

	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileInputStream = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(fileData);
		} finally {
			if (fileInputStream != null)
				fileInputStream.close();
		}
		return fileData;
	}

	private String getContentTypeOfFile(String fileRequested) {
		if (fileRequested.endsWith(".html"))
			return "text/html";
		else if (fileRequested.endsWith(".jpg"))
			return "image/jpeg";
		else if (fileRequested.endsWith(".ico"))
			return "image/icon";
		else
			return "text/plain";
	}

	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer

		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();

		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}

	private void methodNotSupported(PrintWriter out, OutputStream dataOut, String method) throws IOException {
		if (verbose) {
			System.out.println("501 Not Implemented : " + method + " method.");
		}

		// we return the not supported file to the client
		File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
		int fileLength = (int) file.length();
		String contentMimeType = "text/html";
		// read content to return to client
		byte[] fileData = readFileData(file, fileLength);

		// we send HTTP Headers with data to client
		out.println("HTTP/1.1 501 Not Implemented");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + contentMimeType);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer
		// file
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
	}
}
//}
//import java.io.*;
//import java.net.*;
//import java.util.Date;
//
//class TCPServer {
//	public final static int PORT = 8085;
//
//	public static void main(String argv[]) throws Exception {
//		int counter = 0;
//
//		ServerSocket welcomeSocket = new ServerSocket(PORT);
//		System.out.println("Server started!");
//
//		while (true) {
//			Socket clientSocket = welcomeSocket.accept();
//			counter++;
//			System.err.println("New client connected! Count: " + counter + "\n\n\n");
//
//			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
//
//			System.out.println(in);
//			System.out.println(out);
//
//			String s;
//			while ((s = in.readLine()) != null && s.length() > 0) {
//				System.out.println(s);
//				if (s.isEmpty()) {
//					break;
//				}
//			}
//
//			Date date = new Date();
//
//			out.write("HTTP/1.0 200 OK\r\n");
//			out.write("Date: " + date + "\r\n");
//			out.write("Server: Apache/0.8.4\r\n");
//			out.write("Content-Type: text/html\r\n");
//			out.write("Content-Length: 59\r\n");
//			out.write("Expires: Sat, 01 Jan 2050 00:59:59 GMT\r\n");
//			out.write("Last-modified: Fri, 09 Aug 2010 14:21:40 GMT\r\n");
//			out.write("\r\n");
//			out.write("<TITLE>Example</TITLE>");
//			out.write("<P>Ceci n'est pas une page</P>");
//			out.flush();
//
//			System.err.println("Connection closed");
//			out.close();
//			in.close();
//			clientSocket.close();
//		}
////		while (true) {
////			Socket connectionSocket = welcomeSocket.accept();
////			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
////			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
////			String clientSentence = inFromClient.readLine();
////			System.out.println("Received: " + clientSentence);
////			String output = clientSentence.toUpperCase() + '\n';
////		}
////  
//	}
////  outToClient.writeBytes(output); System.out.println("Server end of wile");
//
//}
