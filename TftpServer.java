import java.net.*;
import java.io.*;
import java.util.*;

class TftpServerWorker extends Thread
{
    private DatagramPacket req;
    private static final byte RRQ = 1;
    private static final byte DATA = 2;
    private static final byte ACK = 3;
    private static final byte ERROR = 4;

    private void sendfile(String filename)
    {
        /*
         *Open the file using a FileInputStream and send it, one block at a time to the receiver 
         */

         try{
            byte[] byteArr = new byte[512];
            FileInputStream fileStream = new FileInputStream(filename);
            DatagramSocket ds = new DatagramSocket();
            InetAddress ia = req.getAddress();
            int port = req.getPort();

            int byteR = 0;
            int ByteTransferred = 0;
            while((byteR=fileStream.read(byteArr)) != -1){
                DatagramPacket dp = new DatagramPacket(byteArr, byteR,ia,port);
                if(byteR != 512){
                    System.out.println("Is not 512");
                }
                ds.send(dp);
                ByteTransferred += byteR;
            }

            if(ByteTransferred % 512 ==0){
                byteArr = new byte[0];
                DatagramPacket dp = new DatagramPacket(byteArr, byteArr.length,ia, port);
                ds.send(dp);
                System.out.println("512 is true");
            }
            fileStream.close();
            System.out.println("Transferring Done");
         } catch(Exception e){
            System.out.println("Error: " + e);
         }
         return;
    }

    public void run()
    {
        byte[] buff = req.getData();
        if(buff[0]==RRQ){
            System.out.println("RQQ Request");
            String filename = new String(buff, 1, req.getLength() - 1);
            File file = new File(filename);
            if(file.exists()){
                System.out.println("File Found: " + filename);
                sendfile(filename);
            } else {
                System.out.println("File not Found");
            } 
        }else if (buff[0] == DATA) {
            System.out.println("Data Request");
        }
        
    }

    public TftpServerWorker(DatagramPacket req)
        {
            this.req = req;
        }
}

class TftpServer
{
    public void start_server()
    {
	try {
	    DatagramSocket ds = new DatagramSocket(50000);
	    System.out.println("TftpServer on port " + ds.getLocalPort());

	    for(;;) {
		byte[] buf = new byte[1472];
		DatagramPacket p = new DatagramPacket(buf, 1472);
		ds.receive(p);
		
		TftpServerWorker worker = new TftpServerWorker(p);
		worker.start();
	    }
	}
	catch(Exception e) {
	    System.err.println("Exception: " + e);
	}

	return;
    }

    public static void main(String args[])
    {
	TftpServer d = new TftpServer();
	d.start_server();
    }
}
