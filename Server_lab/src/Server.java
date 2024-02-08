import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.lang.System.out;

public class Server {
    public static final Map configMap = Server.loadConfiguraionFile("./config.ini");
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
        out.println("Server starting...");
        ServerSocket serverSocket = null;
        Socket client = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try{
            serverSocket = new ServerSocket(Integer.parseInt((String)configMap.get("port")));
            out.println("Server is listening on port " + configMap.get("port"));
            while(true){
                client = serverSocket.accept(); // we only use serverSocket to accept the connection
                out.println("New client connected: " + client.getInetAddress().getHostAddress());
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
        OutputStream out = null;
        BufferedReader reader = null;
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = clientSocket.getOutputStream();

                String requestLine = reader.readLine(); // Read the request lin
                if (requestLine == null || requestLine.isEmpty()) {
                    System.out.println("Empty line was received");
                    return;
                }
                System.out.println(requestLine);

                // Parse requestLine to get method and path...
                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];
                String path = requestParts[1];



                // Based on the path, serve the appropriate file or error response
                // Use sendHttpResponse to send the file content or error message

                String rootDirectory = "";
                if (method.equals("GET")) {
                    rootDirectory = (String) configMap.get("root"); // Ensure this is "~/www/lab/html/"
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
                    Server.sendHttpResponse(out, "200 OK", contentType, content);
                }else if (method.equals("POST")){
                    rootDirectory = (String) configMap.get("root"); // Ensure this is "~/www/lab/html/"
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
                    Server.sendHttpResponse(out, "200 OK", contentType, content);
                }else {
                    sendErrorResponse(out, "501 Not Implemented");
                }
            } catch (IOException e) {
                try{
                Server.sendErrorResponse( out,"500 Internal Server Error");
                e.printStackTrace();
                } catch (IOException innerException) {
                    throw new RuntimeException("Failed to send error response", innerException);
                }
            } finally {
                // Close resources: reader, out, clientSocket
                try{
                    reader.close();
                    out.close();
                    clientSocket.close();
                }catch (IOException e){
                    throw new RuntimeException("Failed to close Client thread", e);
                }
            }
        }
    }
    private static void sendHttpResponse(OutputStream out, String status, String contentType, byte[] content) throws IOException {
        String header = "HTTP/1.1 " + status + CRLF +
                "Content-Type: " + contentType + CRLF +
                "Content-Length: " + content.length + CRLF + CRLF;
        System.out.println(header); // Print the header to the console

        PrintWriter headerWriter = new PrintWriter(out, true);
        headerWriter.write(header); // Write the header to the OutputStream
        headerWriter.flush(); // Flush the headers to the OutputStream

        out.write(content); // Write the file content directly to the output stream
        out.flush(); // Ensure all content is written to the OutputStream
    }

    private static void sendErrorResponse(OutputStream out, String status) throws IOException {
            System.out.println("Sending Error Response: " + status);
            String response = "HTTP/1.1 " + status + CRLF + "Content-Type: text/html" + CRLF + CRLF + "<html><body><h1>" + status + "</h1></body></html>";
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