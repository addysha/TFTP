import java.io.*;
import java.net.*;
import java.util.Arrays;

public class TftpClient {
    private static final byte RRQ = 1;
    private static final byte DATA = 2;
    private static final byte ACK = 3;
    private static final byte ERROR = 4;
    private static final int SERVER_PORT = 69; // TFTP server port

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java TftpClient <server_ip> <file_name>");
            return;
        }

        String serverIp = args[0];
        String fileName = args[1];

        try {
            InetAddress serverAddress = InetAddress.getByName(serverIp);
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(1000);

            // Send RRQ (Read Request) packet to the server
            byte[] rrqPacket = createRRQPacket(fileName);
            DatagramPacket rrqDatagram = new DatagramPacket(rrqPacket, rrqPacket.length, serverAddress, SERVER_PORT);
            socket.send(rrqDatagram);

            // Create a file to write the received data
            File outputFile = new File(fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            int blockNumber = 1;

            while (true) {
                byte[] receiveData = new byte[516];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                try {
                    socket.receive(receivePacket);

                    byte[] receivedData = Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength());

                    // Check for ERROR packet
                    if (receivedData[0] == ERROR) {
                        int errorCode = (receivedData[2] << 8) | (receivedData[3] & 0xFF);
                        String errorMessage = new String(Arrays.copyOfRange(receivedData, 4, receivedData.length));
                        System.err.println("Received ERROR packet from server: " + errorCode + " - " + errorMessage);
                        break;
                    }

                    // Check for DATA packet
                    if (receivedData[0] == DATA) {
                        int receivedBlockNumber = ((receivedData[2] & 0xFF) << 8) | (receivedData[3] & 0xFF);

                        if (receivedBlockNumber == blockNumber) {
                            fileOutputStream.write(Arrays.copyOfRange(receivedData, 4, receivedData.length));
                            sendACK(socket, serverAddress, SERVER_PORT, blockNumber);
                            blockNumber++;
                        }
                    }

                    // Check if the received data packet is less than 512 bytes, indicating the end of the file
                    if (receivePacket.getLength() < 516) {
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout waiting for DATA packet.");
                    break;
                }
            }

            // Close the file and the socket
            fileOutputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] createRRQPacket(String fileName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(RRQ);
        try {
            outputStream.write(fileName.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream.write(0); // Null terminator
        try{
            outputStream.write("octet".getBytes("ISO-8859-1")); // TFTP mode
        } catch (IOException e){
            e.printStackTrace();
        }
        
        outputStream.write(0); // Null terminator
        return outputStream.toByteArray();
    }

    private static void sendACK(DatagramSocket socket, InetAddress serverAddress, int serverPort, int blockNumber) {
        byte[] ackPacket = new byte[4];
        ackPacket[0] = ACK;
        ackPacket[1] = (byte) (blockNumber >> 8);
        ackPacket[2] = (byte) blockNumber;
        DatagramPacket ackDatagram = new DatagramPacket(ackPacket, ackPacket.length, serverAddress, serverPort);

        try {
            socket.send(ackDatagram);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
