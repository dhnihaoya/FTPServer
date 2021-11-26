package Server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;

import Server.Enums.*;
import Server.Utils.TransferUtils;

public class WorkingThread extends Thread{

    private boolean quitLoop;
    private final InetAddress address;
    private FTPMode ftpMode;
    private final int dataPort;

    // logging information
    private static final HashMap<String, String> usersAndPass;
    private UserStatus userStatus;
    private String crtUserName;

    // 控制连接
    private PrintWriter controlOut;
    private BufferedReader controlIn;

    //数据连接
    private TransferMode transferMode;//useless
    private TransferType transferType;
    private Socket dataConnection;

    //断开检查
    private long timeStamp;//上一次指令完成时间
    Timer timer;



    static {
        usersAndPass = new HashMap<>();
        usersAndPass.put("test", "test");
    }

    public WorkingThread(Socket controlSocket, int dataPort){
        super();
        System.out.println("initiating a new thread");
        this.timeStamp = System.currentTimeMillis();
        this.timer = new Timer();
        this.ftpMode = FTPMode.ACTIVE;
        this.transferMode = TransferMode.STREAM;
        this.transferType = TransferType.BINARY;
        this.userStatus = UserStatus.NOT_LOGGED_IN;
        this.dataPort = dataPort;
        this.quitLoop = false;
        this.address = controlSocket.getLocalAddress();
        try {
            controlIn = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            controlOut = new PrintWriter(controlSocket.getOutputStream(), true);
        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println("Something is wrong with your io equipment");
        }
    }

    @Override
    public void run() {
        try {
            String crtCommand;
            while (!quitLoop) {
                TimeChecker timeChecker = new TimeChecker(timeStamp);
                timer.schedule(timeChecker, 0, 10000);
                if(timeChecker.needsQuit()){
                    this.quitLoop = true;
                }
                crtCommand = controlIn.readLine();
                if(crtCommand == null){
                    continue;
                }
                doIt(crtCommand);
            }
        }
        catch (IOException e){
            e.printStackTrace();
            sendControlMsg("sth is wrong with IO, I can feel it");
        }
        finally {
            try {
                controlIn.close();
                controlOut.flush();
                System.out.println("stopped this service, retry later if needed");
            } catch (IOException e) {
                System.out.println("Something is so fucking wrong with your io equipment");
                e.printStackTrace();
            }
        }
    }


    private void doIt(String input){
        System.out.println("current command is: " + input);
        String[] cmd = input.split(" ", 2);
        switch (cmd[0]){
            case "USER":
                user(cmd[1]);
                break;
            case "PASS":
                pass(cmd[1]);
                break;
            case "QUIT":
                quit();
                break;
            case "MODE":
                changeTransferMode(cmd[1]);
                break;
            case "PASV":
                toPassiveMode();
                break;
            case "TYPE":
                changeTransferType(cmd[1]);
                break;
            case "PORT":
                connectToPort(cmd[1]);
                break;
            case "STRU":
                connectToPort("202 nothing actually happened, 祝你身体健康");
                break;
            case "STOR":
                storeFile(cmd[1]);
                break;
            case "RETR":
                retrieveFile(cmd[1]);
                break;
            case "NOOP":
                sendControlMsg("202 ok");
                break;
            default:
                sendControlMsg("501 unknown command, 就祝你身体健康吧");
                break;
        }
        //更新上一次命令完成时间
        this.timeStamp = System.currentTimeMillis();
    }


    /**
     * 接受输入的用户名，如果已经登录就拒绝这个请求
     * 然后根据情况修改userStatus
     */
    private void user( String userName ){
        userName = userName.toLowerCase();
        if(userStatus == UserStatus.LEGAL){
            sendControlMsg("530 you have already logged in");
            return;
        }
        //匿名形式登陆
        if( "anonymous".equals(userName) ){
            userStatus = UserStatus.ANONYMOUS;
            sendControlMsg("331 logged in as anonymous account");
        }
        //存在该用户
        else if(usersAndPass.containsKey( userName )){
            userStatus = UserStatus.AUTHED_USERNAME;
            crtUserName = userName;
            sendControlMsg("331 user name " + userName + " received, password is needed");
        }
        else{
            sendControlMsg("530 unknown user name");
        }
    }

    private void pass(String pass){
        if(userStatus == UserStatus.ANONYMOUS){
            sendControlMsg("230 you are anonymous user, any pwd works");
            return;
        }

        if(userStatus != UserStatus.AUTHED_USERNAME){
            sendControlMsg("530 need a username first or you've already logged in");
            return;
        }

        String expectedPwd = usersAndPass.get(crtUserName);
        if(pass.equals(expectedPwd)){
            userStatus = UserStatus.LEGAL;
            sendControlMsg("230 logged in successfully，welcome to our ftp application");
        }
        else{
            sendControlMsg("530 wrong password");
        }
    }

    private void changeTransferMode(String newMode){
        if(this.transferMode == TransferMode.STREAM){
            return;
        }
        newMode = newMode.toLowerCase();
        switch (newMode){
            case "stream":
                this.transferMode = TransferMode.STREAM;
                sendControlMsg("202 you switched to stream mode, nothing happened");
                break;
            case "compressed":
                this.transferMode = TransferMode.COMPRESSED;
                sendControlMsg("202 you switched to compressed mode, nothing happened");
                break;
            case "block":
                this.transferMode = TransferMode.BLOCK;
                sendControlMsg("202 you switched to block mode, nothing happened");
                break;
            default:
                sendControlMsg("503 unknown mode, sry");
                break;
        }
    }

    /**
     * In active mode FTP the client connects from a random unprivileged port (N > 1023)
     * to the FTP server's command port。（21 is given to default ftp）
     * Then, the client starts listening to port N+1 and sends the FTP command PORT N+1 to
     * the FTP server. The server will then connect back to the client's specified data
     * port from its local data port.
     *
     *  In passive mode FTP the client initiates both connections to the server
     *  When opening an FTP connection, the client opens two random unprivileged ports locally
     *  (N > 1023 and N+1). The first port contacts the server on port 21, but instead of then
     *  issuing a PORT command and allowing the server to connect back to its data port, the
     *  client will issue the PASV command. The server then opens a random unprivileged port
     *  (P > 1023) and sends P back to the client in response to the PASV command. The client
     *  then initiates the connection from port N+1 to port P on the server to transfer data.
     *  简而言之 passive mode的时候是server决定port，active的时候是client决定
     */
    private void toPassiveMode(){
        String myIp = this.address.toString().replace('.', ',').replace("/", "");
        System.out.println("Ip is: "+ myIp + ", port is " + this.dataPort);

        int p1 = dataPort / 256;
        int p2 = dataPort % 256;

        sendControlMsg("227 entering passive mode ("+ myIp + "," + p1 +
                ","+ p2 + ")");
        try {
            dataConnection = new ServerSocket(dataPort).accept();
            System.out.println(dataConnection.getRemoteSocketAddress());
            ftpMode = FTPMode.PASSIVE;
            System.out.println("established data connection");
        } catch (IOException e) {
            System.out.println("failed to create data connection");
            e.printStackTrace();
        }
    }

    /**
     * 主动模式下与给定的port连接
     */
    private void connectToPort(String hostAndPort){
        this.ftpMode  = FTPMode.ACTIVE;
        String[] IpAndHost = hostAndPort.split(",");
        String Ip = IpAndHost[0] + "." + IpAndHost[1] + "." + IpAndHost[2] + "." + IpAndHost[3];

        int dataPortNumber = Integer.parseInt(IpAndHost[4]) * 256 + Integer.parseInt(IpAndHost[5]);
        try {
            dataConnection = new Socket(Ip, dataPortNumber);
            sendControlMsg("200 connected to given port(Active mode)");
        }catch (IOException e){
            System.out.println("failed to open socket with:" + Ip +"," + dataPortNumber);
            System.out.println("sth might be wrong with your IO equipment or address");
            e.printStackTrace();
        }
    }


    /**
     * 转换文件传输的格式，有ascii和binary两种选择
     */
    private void changeTransferType(String type){
        switch (type){
            case "ASCII":
                transferType = TransferType.ASCII;
                sendControlMsg("200 changed transfer type to ascii");
                break;
            case "BINARY":
                transferType = TransferType.BINARY;
                sendControlMsg("200 changed transfer type to binary");
                break;
            default:
                sendControlMsg("504 unknown transfer type");
                break;
        }
    }

    /**
     * 用户上传文件至server，控制连接只接文件名，文件内容
     * @param name 文件名
     */
    private void storeFile(String name){
        String folder = System.getProperty("user.dir") + "/files/";

        File file = new File( folder + name );
        if( file.exists() ){
            sendControlMsg("550 file already exist" );
            return;
        }else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedInputStream in = new BufferedInputStream(dataConnection.getInputStream());
            sendControlMsg("150 opening data connection");
            TransferUtils.storeFile(this.transferType, file, in);
            dataConnection.close();
            sendControlMsg("226 file transfer successful");

        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println("failed to initialize inputStream or some error occurred when transferring, damn it");
        }
    }

    private void retrieveFile(String name){
        String folder = System.getProperty("user.dir") + "/files/";
        File file = new File(folder + name);

        if(!file.exists()){
            sendControlMsg("550 file does not exist");
            return;
        }
        try {
            BufferedOutputStream out = new BufferedOutputStream(dataConnection.getOutputStream());
            sendControlMsg("150 opening data connection");
            TransferUtils.retrieveFile(this.transferType, file, out);
            dataConnection.close();
            sendControlMsg("226 file transfer successful");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("failed to initialize outputStream or some error occurred when transferring, damn it");
        }
    }

    private void quit(){
        this.quitLoop = true;
        sendControlMsg("221 bye, see you next time");
        try {
            if (dataConnection != null) {
                dataConnection.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        dataConnection = null;
    }


    private void sendControlMsg(String content){
        controlOut.println(content);
        System.out.println("send: \"" + content + "\" to client");
    }

}



















