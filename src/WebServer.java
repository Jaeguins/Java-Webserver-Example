

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer {
    public static void main(String[] args) {
        ServerSocketHandler serverHandler = new ServerSocketHandler(80);
        serverHandler.turnOn();
    }
}
class SocketHandler{
    Socket socket;
    String sub="";
    BufferedReader reader;
    DataOutputStream writer;
    public SocketHandler(Socket socket){
        this.socket=socket;
        sub+=socket.getInetAddress();
        LogStation.log(sub,"connected");
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new DataOutputStream(socket.getOutputStream());
        }catch(IOException e){e.printStackTrace();}
    }
    public void react()throws IOException{
        try {
            String poppedMsg = readLine();
            String data = "";
            while (!poppedMsg.equals("")) {
                LogStation.log(sub, poppedMsg);
                data += poppedMsg + "\n";
                poppedMsg = readLine();
            }
            HTTPReceiver parser=new HTTPReceiver(data);
            byte[] out;

        switch(parser.target){
            case "/":
                redirect("/html/index.html");
                break;
            default:
                FileInputStream input;
                input=new FileInputStream("res"+parser.target);
                out=new byte[(int)new File("res"+parser.target).length()];
                input.read(out);
                input.close();
                writeData(out);
                break;
        }
        }catch(IOException e){
            notFound();
        }
    }
    public String readLine()throws IOException{
        return reader.readLine();
    }
    public void writeData(String input)throws IOException{
        writeData(input.getBytes());
    }
    public void writeData(byte[] data)throws IOException{
        writer.writeBytes("HTTP/1.1 200 OK \r\n");
        writer.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        writer.writeBytes("Content-Length: " + data.length + "\r\n");
        writer.writeBytes("\r\n");
        writer.write(data, 0, data.length);
        writer.writeBytes("\r\n");
        writer.flush();
    }
    public void redirect(String target)throws IOException{
        writer.writeBytes("HTTP/1.1 301 Moved Permanently\r\n");
        writer.writeBytes("Location: http://127.0.0.1"+target+"\r\n");
        writer.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        writer.writeBytes("\r\n");
        writer.writeBytes("\r\n");
        writer.flush();
    }
    public void notFound()throws IOException{
        byte[] data="404 Not Found".getBytes();
        writer.writeBytes("HTTP/1.1 404 Not Found\r\n");
        writer.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        writer.writeBytes("Content-Length: " + data.length + "\r\n");
        writer.writeBytes("\r\n");
        writer.write(data, 0, data.length);
        writer.writeBytes("\r\n");
        writer.flush();
    }
}
class ServerSocketHandler{
    public ServerSocket serverSocket;
    public boolean serverWaiting=true;
    Queue<SocketHandler> queue= new LinkedList<>();

    public boolean isEmpty(){
        return queue.isEmpty();
    }
    public SocketHandler pop(){
        return queue.poll();
    }
    public ServerSocketHandler(int port){
        try {
            serverSocket=new ServerSocket(port);
        } catch (IOException e) {e.printStackTrace();}
    }
    public void turnOn(){
        serverWaiting=true;
        while(serverWaiting){
            try {
                SocketHandler handler=new SocketHandler(serverSocket.accept());
                queue.offer(handler);
            }catch(IOException e){e.printStackTrace();}
            if(!isEmpty()){
                SocketHandler popped=pop();
                new Thread(() -> {
                    try{
                        popped.react();
                    }catch(IOException e){e.printStackTrace();}
                }).start();
            }
        }
    }
    public void turnOff(){
        serverWaiting=false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class LogStation{//logger
    public static void log(String sub,String msg){
        System.out.println(sub+" : "+msg);
    }
}
class HTTPReceiver{
    String info;
    String target;
    LinkedList<String> tags=new LinkedList<>();
    LinkedList<String> data=new LinkedList<>();
    public HTTPReceiver(String input){
        String[] lines=input.split("\n");
        String[] header=lines[0].substring(0, lines[0].lastIndexOf("HTTP/1.1")).split(" ");
        info=header[0];
        target=header[1];
        for(int i=1;i<lines.length;i++){
            String[] tmp=lines[i].split(": ");
            tags.add(tmp[0]);
            data.add(tmp[1]);
        }

    }
}