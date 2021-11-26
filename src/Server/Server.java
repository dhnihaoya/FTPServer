package Server;

import java.io.IOException;
import java.net.*;


public class Server {
    private ServerSocket controlSocket;
    boolean run = true;

    public static void main(String[] args){
        new Server();
    }

    public Server()  {
        int controlPort = 1025;
        try {
            controlSocket = new ServerSocket(controlPort);

            System.out.println("Server started to listen to port " + controlPort);

        }catch (IOException e){
            e.printStackTrace();
            System.out.println("failed to establish socket");
            System.exit(-1);
        }

        int threadCount = 0;

        while (run){
            try {
                Socket client = controlSocket.accept();

                int dataPort = controlPort + threadCount + 1;
                WorkingThread workingThread = new WorkingThread(client, dataPort);
                System.out.println("New connection received. Worker was created.");
                threadCount++;
                workingThread.start();
            }
            catch (IOException e){
                System.out.println("Exception encountered on accept");
                e.printStackTrace();
            }
        }
        try {
            controlSocket.close();
            System.out.println("closed server");
        }
        catch (IOException e){
            System.out.println("failed to stop server");
            System.exit(-1);
        }
    }



}
