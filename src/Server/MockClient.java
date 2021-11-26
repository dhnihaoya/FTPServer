package Server;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class MockClient {
    Socket controlSocket;
    Socket dataSocket;
    PrintWriter pw;
    BufferedReader br;
    //TODO ip here
    String host = "192.168.5.33";

    public MockClient() throws IOException {
        controlSocket = new Socket( host, 1025);
        System.out.println(controlSocket.getRemoteSocketAddress());
        pw = new PrintWriter(controlSocket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
    }

    //only usable after pasv command
    public String getResp(String command) {
        System.out.println("send: " + command);
        pw.println(command);
        pw.flush();
        String resp = null;
        try {
            resp = br.readLine();
            System.out.println("received: " + resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resp;
    }

    /**
     * 测试上传文件用
     * @param filePath 本地文件路径
     */
    private void testStoreFile(String filePath){
        File file = new File(filePath);
        String cmd = "STOR " + file.getName();
        try {
            getResp(cmd);
            System.out.println("transferring file "+ filePath);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(dataSocket.getOutputStream());
            int len;
            byte[] buffer = new byte[1024];
            while((len = bufferedInputStream.read(buffer)) != -1){
                System.out.println("wrote "+ len + " bytes");
                bufferedOutputStream.write(buffer, 0, len);
                bufferedOutputStream.flush();
            }
            bufferedOutputStream.close();
            System.out.println("finished writing out");
            System.out.println("received: " + br.readLine() );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pasv(){
        String resp = getResp("PASV");

        resp = resp.substring(resp.indexOf("(") + 1, resp.indexOf(")"));

        String[] IPSplit = resp.split(",");
        String hostIp = IPSplit[0] + "." + IPSplit[1] + "." + IPSplit[2] +"."+ IPSplit[3];
        int port = Integer.parseInt(IPSplit[4]) * 256 + Integer.parseInt(IPSplit[5]);
        System.out.println("hostIp: " + hostIp + ", port: " + port);
        try {
            this.dataSocket = new Socket(hostIp, port);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String crtCmd;
        MockClient mockClient = new MockClient();

        String[] splitCmd;
        while(true){
            crtCmd = scanner.nextLine();
            splitCmd = crtCmd.split(" ");

            switch (splitCmd[0]){
                case "PASV":
                    mockClient.pasv();
                    break;
                case "STOR":
                    mockClient.testStoreFile(splitCmd[1]);
                    break;
                default:
                    mockClient.getResp(crtCmd);
                    break;
            }
        }
    }
}
