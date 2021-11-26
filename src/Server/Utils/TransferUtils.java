package Server.Utils;

import Server.Enums.*;

import java.io.*;

public class TransferUtils {

     /**
      * should make sure that the file does not already exist
      * write into server a new file
      * 根据需要的形式调用storeInBinary 和 storeInAscii两个method
      * @param type ascii or binary
      * @param file the file to write into
      * @param in inputStream from socket
      */
     public static void storeFile(TransferType type, File file,
                                     BufferedInputStream in ) throws IOException {
          if(type == TransferType.BINARY){
               storeInBinary(file, in);
          }
          if(type == TransferType.ASCII){
               storeInAscii(file, in);
          }
     }

     private static void storeInBinary(File file, BufferedInputStream reader)
             throws IOException {
          BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(file));

          byte[] buffer = new byte[1024];
          int len;

          while ((len = reader.read(buffer))!= -1){
               System.out.println("wrote " + len + "byte to file");
               writer.write(buffer, 0, len);
               writer.flush();
          }

          reader.close();
          writer.close();
     }

     private static void storeInAscii(File file, BufferedInputStream inputStream)
             throws IOException{
          String crtLine;

          BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
          PrintWriter writer = new PrintWriter(new FileOutputStream(file), true);
          while ((crtLine = reader.readLine()) != null){
               writer.println(crtLine);
          }

          reader.close();
          writer.close();
     }

     public static void retrieveFile(TransferType type, File file, OutputStream out)
             throws IOException{
          if(type == TransferType.ASCII ){
               retrieveInAscii(file, out);
          }

          if(type == TransferType.BINARY){
               retrieveInBinary(file, out);
          }
     }

     private static void retrieveInBinary(File file, OutputStream outputStream)
             throws IOException{
          BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
          BufferedOutputStream writer = new BufferedOutputStream(outputStream);

          byte[] buffer = new byte[1024];
          int length;

          while ((length = reader.read(buffer)) != -1){
               writer.write(buffer, 0, length);
          }

          reader.close();
          writer.close();

     }

     private static void retrieveInAscii(File file, OutputStream outputStream)
             throws IOException{
          BufferedReader reader = new BufferedReader(new FileReader(file));
          PrintWriter writer = new PrintWriter(outputStream, true);

          String crtLine;
          while ((crtLine = reader.readLine()) != null){
               writer.write(crtLine);
          }

          reader.close();
          writer.close();

     }

}
