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
      try {
            // Open the requested file for reading
            FileInputStream fileInputStream = new FileInputStream(filename);

            // Create a new DatagramSocket for sending DATA packets
            DatagramSocket dataSocket = new DatagramSocket();

            // Initialize variables for block number and data
            int blockNumber = 1;
            byte[] data = new byte[512];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(data)) != -1) {
                // Create a DATA packet
                byte[] packetData = new byte[bytesRead + 4];
                packetData[0] = DATA;
                packetData[1] = (byte) (blockNumber >> 8); // High byte of block number
                packetData[2] = (byte) blockNumber;        // Low byte of block number
                System.arraycopy(data, 0, packetData, 3, bytesRead);

                // Create a DatagramPacket with DATA packet and send to the client
                DatagramPacket dataPacket = new DatagramPacket(packetData, packetData.length, req.getAddress(), req.getPort());
                dataSocket.send(dataPacket);

                // Wait for ACK from the client
                byte[] ackData = new byte[4];
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                dataSocket.receive(ackPacket);

                // Check if the received ACK is as expected
                if (ackData[0] != ACK || ackData[1] != (byte) (blockNumber >> 8) || ackData[2] != (byte) blockNumber) {
                    // If not, retransmit the DATA packet
                    continue;
                }

                // Move to the next block
                blockNumber++;

                // If the bytesRead is less than 512, it's the last block
                if (bytesRead < 512) {
                    break;
                }
            }

            // Close the file and the DatagramSocket
            fileInputStream.close();
            dataSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run()
    {
       // Parse the request packet
        byte[] requestData = req.getData();
        if (requestData[0] != RRQ) {
            // Not a RRQ packet, do nothing
            return;
        }

        // Extract the filename from the request packet
        String filename = new String(requestData, 2, req.getLength() - 2); // Subtract 2 for the opcode

        // Call sendfile to send the file to the client
        sendfile(filename);
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
            DatagramSocket ds = new DatagramSocket();
            System.out.println("TftpServer on port " + ds.getLocalPort());

            for (;;) {
                byte[] buf = new byte[1472];
                DatagramPacket p = new DatagramPacket(buf, 1472);
                ds.receive(p);

                TftpServerWorker worker = new TftpServerWorker(p);
                worker.start();
            }
        } catch (Exception e) {
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