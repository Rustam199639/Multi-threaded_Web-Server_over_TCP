import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
public class HTTPRequest implements Runnable{
    private final Socket socket;

    public HTTPRequest(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run(){
        try{
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            BufferedReader reader = new BufferedReader( new InputStreamReader(input));

            String requestLine = reader.readLine();
            System.out.println("Request: " + requestLine);

            StringTokenizer tokenizer = new StringTokenizer(requestLine);
            String method = tokenizer.nextToken().toUpperCase(); // GET or POST or somethihg else need to be in upper case
            String requestURL = tokenizer.nextToken(); // /index.html?x=1&y=2
            String version = tokenizer.nextToken(); // /HHTP


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
