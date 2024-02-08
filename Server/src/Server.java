import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Server {
    public static final Map configMap = Server.loadConfiguraionFile("./config/config.ini");
    private static String CRLF = "\r\n";

    private static Map loadConfiguraionFile(String filePath){
        Properties config = new Properties();
        Map<String, String> hm = new HashMap<String, String>();
        try{
            config.load(new FileInputStream(filePath));
            hm.put("port", config.getProperty("port"));
            hm.put("root", config.getProperty("root"));
            hm.put("defaultPage", config.getProperty("defaultPage"));
            hm.put("maxThreads", config.getProperty("maxThreads"));
        }catch(IOException e){
            e.printStackTrace();
        }
        return hm;
    }
    public static void main(String[] args) {
        System.out.println("Server starting...");
        ServerSocket serverSocket = null;
        Socket client = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            serverSocket = new ServerSocket(Integer.parseInt((String)configMap.get("port")));
            System.out.println("Server is listening on port " + configMap.get("port"));
            while(true){
                client = serverSocket.accept(); // we only use serverSocket to accept the connection
                System.out.println("New client connected: " + client.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(client);
                Thread thread = new Thread(clientHandler);
                thread.start();

                inputStream = client.getInputStream(); // we will read from the browser
                outputStream = client.getOutputStream();
            }



        }catch (IOException e){
            e.printStackTrace();
        }finally {
                try {if (inputStream != null) inputStream.close();}
                catch (IOException e) {e.printStackTrace();}
                try {if (outputStream != null) outputStream.close();}
                catch (IOException e) {e.printStackTrace();}
                try {if(client != null) client.close();}
                catch (IOException e) {e.printStackTrace();}
                try {if (serverSocket != null) serverSocket.close();}
                catch (IOException e) {e.printStackTrace();}
        }
    }
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        // Constructor
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream();

                String requestLine = reader.readLine(); // Read the request line
                System.out.println(requestLine);
                System.out.println(requestLine);

                // Parse requestLine to get method and path...
                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];
                String path = requestParts[1];



                // Based on the path, serve the appropriate file or error response
                // Use sendHttpResponse to send the file content or error message

                // Simple handling for GET requests only in this example
                if (!method.equals("GET")) {
                    sendErrorResponse(out, "501 Not Implemented");
                    return;
                }

                String rootDirectory = (String) configMap.get("root"); // Ensure this is "~/www/lab/html/"
                String resolvedPath = path.equals("/") ? rootDirectory + configMap.get("defaultPage") : rootDirectory + path;
                String filePath = URLDecoder.decode(resolvedPath, "UTF-8");
                File file = new File(filePath);

                if (!file.exists() || file.isDirectory()) {
                    sendErrorResponse(out, "404 Not Found");
                    return;
                }

                // Read the file content and determine the content type
                byte[] content = Files.readAllBytes(file.toPath());
                String contentType = getContentType(file);


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Close resources: reader, out, clientSocket
            }
        }
    }
    private static void sendHttpResponse(OutputStream out, String status, String contentType, byte[] content) throws IOException {
        PrintWriter headerWriter = new PrintWriter(out, true);
        headerWriter.write("HTTP/1.1 " + status + CRLF);
        headerWriter.write("Content-Type: " + contentType + CRLF);
        headerWriter.write("Content-Length: " + content.length + CRLF);
        headerWriter.write(CRLF); // Blank line between headers and content, very important!
        headerWriter.flush(); // Flush the headers to the OutputStream

        out.write(content); // Write the file content directly to the output stream
        out.flush(); // Ensure all content is written to the OutputStream
    }
    private static void sendErrorResponse(OutputStream out, String status) throws IOException {
        String response = "HTTP/1.1 " + status + CRLF +"Content-Type: text/html"+CRLF+CRLF+"<html><body><h1>" + status + "</h1></body></html>";
        out.write(response.getBytes());
    }
    private static String getContentType(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html";
        else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        else if (fileName.endsWith(".png")) return "image/png";
            // Add other content types as needed
        else return "application/octet-stream"; // Default binary file type
    }
}