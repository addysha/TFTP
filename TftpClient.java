import java.net.*;
import java.io.*;

class TftpClient
{
    private static final byte RRQ = 1;
    private static final byte DATA = 2;
    private static final byte ACK = 3;
    private static final byte ERROR = 4;

    public static void main (String[] args){
        if (args.length !=3){
            System.err.println("<Client> <ip> <port> <String>");
            return;
        }
        try{
            InetAddress ia = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);
            String filename = args[2].toString();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(RRQ);
            output.write(filename.getBytes());
            byte[] buff = new byte[512];
            buff = output.toByteArray();

            System.out.println("Initial Byte " + buff[0]);
            System.out.println(buff.length);
            DatagramPacket dp = new DatagramPacket(buff,buff.length,ia,port);
            DatagramSocket ds = new DatagramSocket();
            ds.send(dp);

            File Filereci = new File("Client/", filename);
            Filereci.createNewFile();
            FileOutputStream outputFile = new FileOutputStream(Filereci);

            for(;;){
                byte[] outputBuffer = new byte[512];
                DatagramPacket dpack = new DatagramPacket(outputBuffer,512);
                ds.receive(dpack);
                outputBuffer = dpack.getData();
                outputFile.write(outputBuffer);
                outputFile.flush();
                if(dpack.getLength() !=512){
                    System.out.println("Closed Recieving");
                    break;
                }else{

                }
            }
            outputFile.flush();
            outputFile.close();

        } catch(Exception e){
            System.err.println("Error: " + e);
        }
    }
}
