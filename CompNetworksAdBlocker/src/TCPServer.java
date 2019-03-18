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
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TCPServer implements Runnable {
	static final int PORT = 80;
	private static int connectCounter = 0;
	private Socket socket;

	private boolean keepAlive = true;

	static final File WEB_ROOT = new File("webContent/");
	static final File FILE_ROOT = new File("saveFiles/");

	static final String DEFAULT_FILE = "index.html";
	static final String BAD_REQUEST = "400.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String SERVER_ERROR = "500.html";

	static final boolean verbose = true;

	public TCPServer(Socket port) throws SocketException {
		this.socket = port;
		socket.setKeepAlive(true);
	}

	public static void main(String[] args) throws IOException {
		ServerSocket serverConnect = null;

		try {
			serverConnect = new ServerSocket(PORT);
			if (verbose)
				System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

			while (true) {
				TCPServer myServer = new TCPServer(serverConnect.accept());

				if (verbose)
					System.out.println("New connection! Connection counter: " + ++connectCounter + "\n");

				Thread thread = new Thread(myServer);
				thread.start();
			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		} finally {
			serverConnect.close();
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

			// Reading first line
			String[] parsedFirstLine = inputStream.readLine().split(" ");
			method = parsedFirstLine[0].toUpperCase();
			fileRequested = parsedFirstLine[1].toLowerCase();
			httpVersion = parsedFirstLine[2].toUpperCase();

			this.keepAlive &= httpVersion.equals("HTTP/1.1");
			if (verbose && !this.keepAlive)
				System.out.println("Http version is not 1.1, closing after request");

			String s = null, host = null, modifiedTime = null;
			int contentLength = 0;

			String hostString = "Host: ";
			String contentLenghtString = "Content-Length: ";
			String connectionString = "Connection: ";
			String modifiedString = "If-Modified-Since: ";

			// Reading rest of header
			while ((s = inputStream.readLine()) != null && s.length() > 0) {
				if (s.contains(hostString))
					host = s.substring(hostString.length());

				if (s.contains(contentLenghtString))
					contentLength = Integer.parseInt(s.substring(contentLenghtString.length()));

				if (s.contains(connectionString))
					this.keepAlive &= s.substring(connectionString.length()).contentEquals("keep-alive");

				if (s.contains(modifiedString))
					modifiedTime = s.substring(modifiedString.length());

				if (s.isEmpty()) {
					break;
				}
			}

			// Client does not include host header with HTTP 1.1
			if (httpVersion.equals("HTTP/1.1") && (host == null || host.isEmpty())) {
				throw new Exception("400 Bad request");
			}

			switch (method) {
			case "HEAD":
				methodHead(outputStream, fileRequested);
				break;
			case "GET":
				methodGet(outputStream, dataOutputStream, fileRequested, modifiedTime);
				break;
			case "PUT":
				methodPUT(inputStream, outputStream, dataOutputStream, fileRequested, contentLength);
				break;
			case "POST":
				methodPOST(inputStream, outputStream, dataOutputStream, fileRequested, contentLength);
				break;
			default:
				throw new Exception("500 HTTP Method not supported");
			}

		} catch (FileNotFoundException e) {

			try {
				// 404
				fileNotFound(outputStream, dataOutputStream, fileRequested);
			} catch (IOException e2) {
				System.err.println("Error with file not found catch : " + e2.getMessage());
			}

		} catch (Exception e) {
			try {

				if (e.getMessage().contains("400")) {
					badRequest(outputStream, dataOutputStream);
				} else {
					// 500
					serverError(outputStream, dataOutputStream);
				}
			} catch (IOException e2) {
				System.err.println("Error with server error catch : " + e2.getMessage());
			}
		} finally {
			try {
				if (!this.keepAlive) {
					inputStream.close();
					outputStream.close();
					dataOutputStream.close();
					socket.close();

					if (verbose) {
						System.out.println("Connection closed");
						System.out.println("----------------------\n");
					}
				}

				if (verbose)
					System.out.println("socket closed? =" + socket.isClosed());

			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}
		}
	}

	private void methodHead(PrintWriter out, String fileRequested) throws ParseException, FileNotFoundException {
		if (fileRequested.equals("/"))
			fileRequested = DEFAULT_FILE;

		File file = new File(WEB_ROOT, fileRequested);

		if (!file.exists())
			throw new FileNotFoundException("File doesn't exist!");

		String contentType = getContentTypeOfFile(fileRequested);
		int contentLength = (int) file.length();

		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		Date fileDate = format.parse(format.format(file.lastModified()));

		out.println("HTTP/1.1 200 OK");
		out.println("Date: " + new Date());
		out.println("Server: Java HTTP Server");
		out.println("Content-Type: " + contentType);
		out.println("Content-Length: " + contentLength);
		out.println("Last-Modified: " + fileDate);
		out.println();
		out.flush();

		if (verbose)
			System.out.println("HEAD: File " + fileRequested + " of type " + contentType + " returned");

	}

	private void methodPOST(BufferedReader inputStream, PrintWriter outputStream, BufferedOutputStream dataOutputStream,
			String fileRequested, int contentLength) throws NumberFormatException, IOException {
		File file = new File(FILE_ROOT, fileRequested);
		if (file.createNewFile()) {
			if (verbose)
				System.out.println("File not yet found. Creating new file!");
			file.delete();
			this.methodPUT(inputStream, outputStream, dataOutputStream, fileRequested, contentLength);
		} else {

			if (verbose)
				System.out.println("Going to edit file " + file.getCanonicalFile());

			FileWriter fileWriter = new FileWriter(file, true);

			char[] buffer = new char[contentLength];
			int amountRead = inputStream.read(buffer, 0, contentLength);
			if (amountRead == contentLength) {
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
			outputStream.println("Content-Type: " + getContentTypeOfFile(fileRequested));
			outputStream.println("Content-Length: " + newContentLength);
			outputStream.println();
			outputStream.flush();

			byte[] data = readFileData(file, newContentLength);
			dataOutputStream.write(data, 0, newContentLength);
			dataOutputStream.flush();
		}
	}

	private void methodGet(PrintWriter out, BufferedOutputStream dataOut, String fileRequested, String modifiedTime)
			throws IOException, ParseException {
		// index.html & index & /
		if (fileRequested.equals("/") || fileRequested.equals("index"))
			fileRequested = DEFAULT_FILE;

		File file = new File(WEB_ROOT, fileRequested);

		if (!file.exists())
			throw new FileNotFoundException("File doesn't exist!");

		Date requestDate = null, fileDate = null;

		// If given a If-Modified-Since header date
		if (modifiedTime != null) {
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

			requestDate = format.parse(modifiedTime);
			fileDate = format.parse(format.format(file.lastModified()));
		}

		// If no If-Modified-Since was given or valid requestDate return 200
		if (modifiedTime == null || requestDate.after(fileDate)) {
			out.println("HTTP/1.1 200 OK");
			out.println("Date: " + new Date());
			out.println("Server: Java HTTP Server");
			out.println("Content-Type: " + getContentTypeOfFile(fileRequested));
			out.println("Content-Length: " + (int) file.length());
			out.println("Last-Modified: " + fileDate);
			out.println();
			out.flush();

			byte[] data = readFileData(file, (int) file.length());
			dataOut.write(data, 0, (int) file.length());
			dataOut.write(0);
			dataOut.flush();

			if (verbose)
				System.out.println("GET: File " + fileRequested + " returned");

		} else {
			// Else return 304
			out.println("HTTP/1.1 304 NOT MODIFIED");
			out.println("Date: " + new Date());
			out.println("Server: Java HTTP Server");
			out.println("Content-Type: " + getContentTypeOfFile(fileRequested));
			out.println("Content-Length: " + (int) file.length());
			out.println("Last-Modified: " + fileDate);
			out.println();
			out.flush();

			if (verbose)
				System.out.println(
						"GET: File " + fileRequested + " not returned since modified date wasn't more recent.");
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
			outputStream.println("Content-Type: " + getContentTypeOfFile(fileRequested));
			outputStream.println("Content-Length: " + contentLength);
			outputStream.println();
			outputStream.flush();

			byte[] data = readFileData(file, contentLength);
			dataOut.write(data, 0, contentLength);
			dataOut.write(0);
			dataOut.flush();

		} else {
			badRequest(outputStream, dataOut);
		}
	}

	private void badRequest(PrintWriter out, OutputStream dataOut) throws IOException {
		if (verbose) {
			System.out.println("HTTP 400 BAD REQUEST");
		}

		File file = new File(WEB_ROOT, BAD_REQUEST);
		int fileLength = (int) file.length();
		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.1 400 BAD REQUEST");
		out.println("Server: Java HTTP Server");
		out.println("Date: " + new Date());
		out.println("Content-Type: text/html");
		out.println("Content-Length: " + fileLength);
		out.println();
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.write(0);
		dataOut.flush();
	}

	private void serverError(PrintWriter out, OutputStream dataOut) throws IOException {
		if (verbose) {
			System.out.println("HTTP 500 SERVER ERROR");
		}

		File file = new File(WEB_ROOT, SERVER_ERROR);
		int fileLength = (int) file.length();
		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.1 500 SERVER ERROR");
		out.println("Server: Java HTTP Server");
		out.println("Date: " + new Date());
		out.println("Content-Type: text/html");
		out.println("Content-Length: " + fileLength);
		out.println();
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.write(0);
		dataOut.flush();

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
		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.1 404 NOT FOUND");
		out.println("Server: Java HTTP Server");
		out.println("Date: " + new Date());
		out.println("Content-Type: text/html");
		out.println("Content-Length: " + fileLength);
		out.println();
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.write(0);
		dataOut.flush();

		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}

}
//}
//import java.io.*;
//import java.net.*;
