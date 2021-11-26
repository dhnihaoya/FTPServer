package Server;

import java.io.*;
import java.net.Socket;

public class ClientSock{
    public ClientSock() throws IOException {
        Socket socket = new Socket("192.168.2.197" , 1025);
        System.out.println(socket.getRemoteSocketAddress());
        OutputStream os = socket.getOutputStream();
        PrintWriter pw = new PrintWriter(os);

        InputStream is = socket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        System.out.println("received " + br.readLine());


        String info = "write sth";
        System.out.println(info);
        pw.write(info);
        pw.flush();

        socket.shutdownOutput();

        String reply;
        while (!((reply=br.readLine())==null)){
            System.out.println("received " + reply);
        }

        br.close();
        is.close();
        pw.close();
        os.close();
        socket.close();
    }


    public static void main(String[] args) throws IOException {
        ClientSock clientSock = new ClientSock();
    }
}
