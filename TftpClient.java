import java.net.*;
import java.util.Arrays;
import java.io.*;

/**
 * @author Aditya Sharma 
 * This class represents a TFTP client that can send a Read Request (RRQ) for a file to a TFTP server,
 * receive the file data, and save it to a local file.
 */
class TftpClient {
    private static final byte RRQ = 1;
    private static final byte DATA = 2;
    private static final byte ACK = 3;
    private static final byte ERROR = 4;

    // Set static Inet ia variable
    private static InetAddress ia;
    // set port variable
    private static int port;
    // set block number to be 1 
    private static byte blockNum = 1;

    /**
     * The main method for the TFTP client.
     *
     * @param args Command line arguments. Expects three arguments: <ip> <port> <filename>.
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("<Client> <ip> <port> <filename>");
            return;
        }
        try {
            ia = InetAddress.getByName(args[0]);
            port = Integer.parseInt(args[1]);
            String filename = args[2].toString();
            // Create a ByteArrayOutputStream to construct the Read Request (RRQ) packet
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            // Write the TFTP RRQ packet type (1) to the output stream.
            output.write(RRQ);
            // Write the requested filename as bytes to the output stream.
            output.write(filename.getBytes());
            // Convert the output stream to a byte array.
            byte[] buff = new byte[512];
            buff = output.toByteArray();
            
            // Create a DatagramPacket containing the RRQ packet to send to the server.
            DatagramPacket dp = new DatagramPacket(buff, buff.length, ia, port);
            // Create a DatagramSocket for communication with the server.
            DatagramSocket ds = new DatagramSocket();
            // Set a socket timeout of 1000 milliseconds (1 second) for receiving packets.
            ds.setSoTimeout(1000);
            // Send the RRQ packet to the server.
            ds.send(dp);

            // Create a new file for writing the received data to rx.
            File inputFile = new File("Client/" + filename);
            inputFile.createNewFile();
            // Create a FileOutputStream for writing data to the file.
            FileOutputStream outputFile = new FileOutputStream(inputFile);
            // Receive the initial data packet or error packet from the server.
            byte[] outputBuffer = DataAcquired(ds);

            // Check if the received packet is a TFTP DATA packet (2).
            if (outputBuffer[0] == DATA) {

                // Continue receiving data until a packet with less than 514 bytes is received.
                while (outputBuffer.length == 514) {
                     // Exclude the TFTP header (2 bytes) and write the data to the output file.
                    outputBuffer = Arrays.copyOfRange(outputBuffer, 2, outputBuffer.length);
                    outputFile.write(outputBuffer);
                    outputFile.flush();

                    // Receive the next data packet from the server.
                    outputBuffer = DataAcquired(ds);
                }
                // --- for buffer length check
                // System.out.println("Output buffer length was < 514: " + outputBuffer.length);
                // Exclude the TFTP header (2 bytes) and write the remaining data to the output file.
                outputBuffer = Arrays.copyOfRange(outputBuffer, 2, outputBuffer.length);
                outputFile.write(outputBuffer);
                outputFile.flush();
                // Check if the received packet is a TFTP ERROR packet (4).
            } else if (outputBuffer[0] == ERROR) {
                // Extract the error message from the packet (excluding the opcode).
                String errorMessage = new String(outputBuffer, 1, outputBuffer.length - 1);
                // Print the error message.
                System.out.print(errorMessage);
            }
            // Flush and close the output file.
            outputFile.flush();
            outputFile.close();
            // Close the DatagramSocket.
            ds.close();
        } catch (Exception e) {
            // Handle any exceptions that may occur during the process.
            System.err.println("Error: " + e);
        }
    }

    /**
     * Sends an acknowledgment (ACK) packet to the server.
     *
     * @param ds The DatagramSocket for sending the ACK packet.
     */
        public static void sendAck(DatagramSocket ds) {
        // Create a ByteArrayOutputStream to construct the ACK packet.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // Write the TFTP ACK packet type (3) to the output stream
        output.write(ACK);
        // Write the current block number to the output stream.
        output.write(blockNum);
        // Convert the output stream to a byte array.
        byte[] buffer = output.toByteArray();
        // Create a DatagramPacket containing the ACK packet to send to the server.
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length, ia, port);
        try {
            // Send the ACK packet to the server using the DatagramSocket.
            ds.send(dp);
        } catch (Exception e) {
            // Handle any exceptions that may occur during the sending process.
            System.err.println("Error: " + e);
        }
    }

    /**
     * Receives data from the server.
     *
     * @param ds The DatagramSocket for receiving data packets.
     * @return The received data as a byte array.
     */
    private static byte[] DataAcquired(DatagramSocket ds) {
        // Create a byte array to store received data (maximum size of 514 bytes).
        byte[] outputBuffer = new byte[514];
        // Create a DatagramPacket to receive data into the outputBuffer.
        DatagramPacket dp = new DatagramPacket(outputBuffer, 514);
        try {
            // Receive a packet from the server using the DatagramSocket.
            ds.receive(dp);
            // Update the InetAddress and port for potential use in subsequent operations.
            ia = dp.getAddress();
            port = dp.getPort();
        } catch (IOException e) {
            // If an IOException occurs (e.g., timeout), send an acknowledgment (ACK).
            sendAck(ds);
        }
        // Create a byte array to store the received data without extra padding.
        byte[] allBytes = new byte[dp.getLength()];
        // Copy the received data from the DatagramPacket into allBytes.
        byte[] tempBytes = dp.getData();
        allBytes = Arrays.copyOfRange(tempBytes, 0, dp.getLength());
        // Print the number of bytes collected.
        System.out.println("Collected bytes : " + allBytes.length);

        if (allBytes[0] == DATA) {
            // Check if the received packet is a TFTP DATA packet (2).
            sendAck(ds); // Send an acknowledgment (ACK) in response to the received data.
            // Check if the received block number matches the expected block number.
            if (allBytes[1] != blockNum) {
                System.out.println("Not matching block number");
                allBytes = DataAcquired(ds); // Request retransmission of the missing block.
            }
            blockNum++;// Increment the expected block number for the next data packet.
        }
        return allBytes; // Return the received data.
    }
}
