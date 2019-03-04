import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Date;
import java.util.StringTokenizer;

public class TCPServer implements Runnable {
	static final int PORT = 8081;
	private Socket connect;

	static final File WEB_ROOT = new File("webContent/");
	static final String DEFAULT_FILE = "index.html"; // 200

	static final String NOT_MODIFIED = "304.html";
	static final String BAD_REQUEST = "400.html";
	static final String METHOD_NOT_SUPPORTED = "405.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String SERVER_ERROR = "500.html";

	static final boolean verbose = true;

	public TCPServer(Socket c) {
		connect = c;
	}

	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

			while (true) {
				TCPServer myServer = new TCPServer(serverConnect.accept());

				if (verbose)
					System.out.println("New connection! (" + new Date() + ")\n");

				// create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		String fileRequested = null;
		String method = null;

		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// we get character output stream to client (for headers)
			out = new PrintWriter(connect.getOutputStream());
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());

			// we parse the first line of the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(in.readLine());

			method = parse.nextToken().toUpperCase();
			fileRequested = parse.nextToken().toLowerCase();

			switch (method) {
			case "HEAD":
				methodHeadGet(out, dataOut, fileRequested, false);
				break;
			case "GET":
				methodHeadGet(out, dataOut, fileRequested, true);
				break;
			case "PUT":
				// methodPUT(out, dataOut);
				break;
			case "POST":
				// methodPOST(out, dataOut);
				break;
			default:
				methodNotSupported(out, dataOut, method);
				break;

			}
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}

		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close();
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}

			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}

	}

	private void methodHeadGet(PrintWriter out, BufferedOutputStream dataOut, String fileRequested, boolean isGet)
			throws IOException {
		if (fileRequested.endsWith("/")) {
			fileRequested += DEFAULT_FILE;
		}

		File file = new File(WEB_ROOT, fileRequested);
		int fileLength = (int) file.length();
		String content = getContentType(fileRequested);

		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.1 200 OK");
		out.println("Server: Java HTTP Server");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);

		if (isGet) {
			out.println();
			out.flush();
			dataOut.write(fileData, 0, fileLength); //TODO check dit
			dataOut.flush();
		}
		if (verbose) {
			System.out.print(isGet ? "GET" : "HEAD");
			System.out.println(": File " + fileRequested + " of type " + content + " returned");
		}

	}

	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return fileData;
	}

	// return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
			return "text/html";
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

/*
 * import java.io.*; import java.net.*; import java.util.Date;
 * 
 * class TCPServer { public final static int PORT = 6782;
 * 
 * public static void main(String argv[]) throws Exception { int counter = 0;
 * 
 * ServerSocket welcomeSocket = new ServerSocket(PORT);
 * System.out.println("Server started!");
 * 
 * while (true) { Socket clientSocket = welcomeSocket.accept(); counter++;
 * System.err.println("New client connected! Count: " + counter + "\n\n\n");
 * 
 * BufferedReader in = new BufferedReader(new
 * InputStreamReader(clientSocket.getInputStream())); BufferedWriter out = new
 * BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
 * 
 * System.out.println(in); System.out.println(out);
 * 
 * String s; /*while ((s = in.readLine()) != null && s.length() > 0) {
 * System.out.println(s); if (s.isEmpty()) { break; } }
 * 
 * Date date = new Date();
 * 
 * out.write("HTTP/1.0 200 OK\r\n"); out.write("Date: " + date + "\r\n");
 * out.write("Server: Apache/0.8.4\r\n");
 * out.write("Content-Type: text/html\r\n");
 * out.write("Content-Length: 59\r\n");
 * out.write("Expires: Sat, 01 Jan 2050 00:59:59 GMT\r\n");
 * out.write("Last-modified: Fri, 09 Aug 2010 14:21:40 GMT\r\n");
 * out.write("\r\n"); out.write("<TITLE>Example</TITLE>");
 * out.write("<P>Ceci n'est pas une page</P>");
 * 
 * System.err.println("Connection closed"); out.close(); in.close();
 * clientSocket.close(); } } }
 * 
 * /* while (true) { Socket connectionSocket = welcomeSocket.accept();
 * BufferedReader inFromClient = new BufferedReader(new
 * InputStreamReader(connectionSocket.getInputStream())); DataOutputStream
 * outToClient = new DataOutputStream(connectionSocket.getOutputStream());
 * String clientSentence = inFromClient.readLine();
 * System.out.println("Received: " + clientSentence); String output =
 * clientSentence.toUpperCase() + '\n';
 * 
 * 
 * 
 * 
 * outToClient.writeBytes(output); System.out.println("Server end of wile");
 * 
 * }
 */