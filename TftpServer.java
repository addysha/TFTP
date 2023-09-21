import java.net.*;
import java.io.*;

/**
 * @author Aditya Sharma 
 * This class represents a worker thread for a TFTP server. It handles incoming TFTP requests,
 * processes read requests (RRQ), and sends requested files to the client.
 */
class TftpServerWorker extends Thread {

    private DatagramPacket req;

    // TFTP packet types
    private static final byte RRQ = 1;
    private static final byte DATA = 2;
    private static final byte ACK = 3;
    private static final byte ERROR = 4;

    //block number
    private byte blockNum = 1;
    private byte requesting = 0;

    /**
     * Sends a packet and waits for an acknowledgment.
     *
     * @param ds The DatagramSocket for sending and receiving packets.
     * @param dp The DatagramPacket to send.
     */
    private void SendPkt(DatagramSocket ds, DatagramPacket dp) {
        // If total connection requests are 5 return off 
        if (requesting == 5) {
            return;
        } else {
            try {
                // send Data packet to datasocket
                ds.send(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // waits for the acknowledgement 
            HoldAck(ds, dp);
        }
    }

    /**
     * Holds the ack for waiting 
     *
     * @param ds The DatagramSocket for sending and receiving packets.
     * @param dp The DatagramPacket to send.
     */
    private void HoldAck(DatagramSocket ds, DatagramPacket dp) {
        byte[] BufferAck = new byte[1472];
        DatagramPacket p = new DatagramPacket(BufferAck, 1472);
        try {
            ds.receive(p);
            requesting = 0; // Reset the number of consecutive timeouts upon successful receipt.

        } catch (IOException e) {
            System.out.println("Timeout! Nothing received.");
            requesting++; 
            if (requesting == 5) {
                System.out.println("Total attempts are now 5");
                return; // If 5 consecutive timeouts occur, exit the method.
            } else {
                SendPkt(ds, dp);  // Resend the packet when a timeout occurs.
            }
        }
        BufferAck = p.getData(); // Get the received data from the packet.

         // Check if the received packet is not an ACK with the expected block number.
        if (!(BufferAck[0] == ACK && BufferAck[1] == blockNum)) {

            if (requesting == 5) {
                return;// If 5 consecutive timeouts occur, exit the method.
            }
            // Update the expected block number.
            blockNum = BufferAck[1];
            // Resend the packet when the expected ACK is not received.
            SendPkt(ds, dp);

        } else {
            // Increment the block number for the next data packet.
            blockNum++;
        }
    }

    /**
     * Sends a file to the client in TFTP DATA packets.
     *
     * @param filename The name of the file to send.
     */
    private void sendfile(String filename) {
        try {
            // New byte Array of 512
            byte[] byteArr = new byte[512];
            // New fileStream object
            FileInputStream fileStream = new FileInputStream(filename);
            // New datagramSocket ds
            DatagramSocket ds = new DatagramSocket();
            // Set the time out for the ds to 1 second
            ds.setSoTimeout(1000);
            // new ia Internet Address calling for address
            InetAddress ia = req.getAddress();
            // port variable to get port numb
            int port = req.getPort();
            // set byte reader to 0
            int byteR = 0;
            // set total bytes sent to 0 
            int ByteTransferred = 0;
            
            // Read a chunk of data from the file into byteArr.
            // Continue the loop until the end of file or until 5 attempts have been made.
            while (((byteR = fileStream.read(byteArr)) != -1) && requesting != 5) {

                ByteArrayOutputStream outPut = new ByteArrayOutputStream();
                outPut.write(DATA); // Write the TFTP DATA packet type (2).
                outPut.write(blockNum);  // Write the current block number.
                outPut.write(byteArr); // Write the data from the file.
                byte[] buffer = outPut.toByteArray(); // set byte buffer to output.toByteArry

                // Create a DatagramPacket to send the data to the client.
                DatagramPacket dp = new DatagramPacket(buffer, byteR + 2, ia, port);
                // Send the data packet to the client.
                SendPkt(ds, dp);
                // Increment all the total bytes transferred.
                ByteTransferred += byteR;
            }
            // If the total bytes transferred is a multiple of 512 and we haven't reached 5 attempts.
            if ((ByteTransferred % 512 == 0) && requesting != 5) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                output.write(DATA); // Write the TFTP DATA  acket type (2).
                output.write(blockNum); // Write the current block number.
                byte[] buf = output.toByteArray(); // set byte buffer to output.toByteArry

                // Create a DatagramPacket to send an empty DATA packet (end of file marker).
                DatagramPacket dp = new DatagramPacket(buf, buf.length, ia, port);
                SendPkt(ds, dp);// Send the empty DATA packet to the client.
            } 
            fileStream.close(); // Close the input file stream.
            ds.close(); // Close the DatagramSocket.
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        return; // Return from the method.
    }

    /**
     * Runs the TFTP server worker thread.
     */
    public void run() {
        // Check if the received packet is a Read Request (RRQ).
        byte[] buff = req.getData();
        if (buff[0] == RRQ) {
            System.out.println("RRQ Request");
            // Extract the requested filename from the packet data.
            String filename = new String(buff, 1, req.getLength() - 1);
            File file = new File(filename);
            // Check if the requested file exists on the server.
            if (file.exists()) {
                
                System.out.println("File Found: " + filename);
                // If the file exists, send it to the client using the sendfile method.
                sendfile(filename);
            } else {
                // If the file doesn't exist, print an error message.
                System.out.println("Received RRQ file doesn't exist: " + filename);
                try {
                    DatagramSocket ds = new DatagramSocket();
                    InetAddress ia = req.getAddress();
                    int port = req.getPort();

                    //Create a new String with error message
                    String errorMessage = "File is not on the server";
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    output.write(ERROR);// Write the TFTP ERROR packet type (4).
                    output.write(errorMessage.getBytes());// Write the error message.
                    buff = output.toByteArray();

                    DatagramPacket dp = new DatagramPacket(buff, buff.length, ia, port);
                    ds.send(dp);
                    // Create and send an ERROR packet to the client indicating that the file is not found.
                } catch (Exception e) {
                    System.out.println("Error: " + e);
                }
            }
        }
    }

    /**
     * Constructor for TftpServerWorker.
     *
     * @param req The DatagramPacket containing the request.
     */
    public TftpServerWorker(DatagramPacket req) {
        this.req = req;
    }
}

/**
 * This class represents a simple TFTP server that listens for incoming requests and
 * processes them using worker threads.
 */
class TftpServer {
    /**
     * Starts the TFTP server and listens for incoming requests.
     */
    public void start_server() {
        try {
            // New Datagram socket ds 
            DatagramSocket ds = new DatagramSocket(50000);
            System.out.println("TFTP Server on port " + ds.getLocalPort());

            //Infinite For loop 
            for (; ; ) {
                // new byte buffer set to 1472
                byte[] buf = new byte[1472];
                DatagramPacket p = new DatagramPacket(buf, 1472);
                ds.receive(p);
                // New worker object 
                TftpServerWorker worker = new TftpServerWorker(p);
                worker.start();
            }
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }

    /**
     * Main method to start the TFTP server.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String args[]) {
        // new Server object
        TftpServer server = new TftpServer();
        //starts server 
        server.start_server();
    }
}
