import java.io.IOException;
import java.net.UnknownHostException;

public class App {
	private static TCPClient client;

	public static void main(String[] args) throws Exception {
		client = new TCPClient();
		client.parseParam(args);
		client.run();

	}

}
