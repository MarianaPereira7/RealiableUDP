package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

import static java.lang.Integer.parseInt;

public class UDPClient {
    private DatagramSocket udpSocket;
    private byte[] receiveData = new byte[bufferLength];
    private byte[] sendData = new byte[bufferLength];
    private static int bufferLength = 20;

    /**
     * Creates a DatagramSocket and sets its reception timeout
     *
     * @param timeout Timeout set for packet reception
     */
    public UDPClient(int timeout) {
        try{
            udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(timeout);
        }catch (SocketException e) {
            System.err.println("Socket error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public int sendMessage(String message, InetAddress address, int port){
        //Calculate number of fragments, in case buffer length is lower than the message length
        int messageLength = message.length();
        int numberOfFragments = (int) Math.ceil((double) messageLength / bufferLength);
        String[] fragmentedMessage = divideMessage(message,numberOfFragments);

        if(sendReliablePacket("Length:" + numberOfFragments,address,port) == -1){
            return -1;
        }

        for(int i = 0; i < numberOfFragments; i++){
            if(sendReliablePacket(fragmentedMessage[i],address,port) == -1){
                return -1;
            }
        }
        return 0;
    }

    private String[] divideMessage(String message, int numberOfFragments){
        String[] fragments = new String[numberOfFragments];
        if(numberOfFragments == 1){
            fragments[0] = message;
            return fragments;
        }
        for(int i = 0; i < numberOfFragments; i++){
            int start = i * bufferLength;
            int end = Math.min((i + 1) * bufferLength, message.length());
            fragments[i] = message.substring(start,end);
        }
        return fragments;
    }


    /**
     * Repeatedly reads a line from terminal, sends it to a server living at hostname:port, and waits for a reply
     * Use CTRL + D to exit
     *
     *  @param address IP address of the UDP server
     *  @param port Port binded to the UDP server living at hostname
     */
    private int sendReliablePacket(String message, InetAddress address, int port) {
        int counter = 0;
        try{
            String received = "NONE";

            while (!received.equals("ACK") && counter < 3) {
                counter++;
                sendPacket(address,port,message);
                received = receivePacket();
            }

        }catch(SocketTimeoutException e){
            System.err.println("Timeout reached: " + e.getMessage());
        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }

        //To validate if the message was successfully sent:
        if(counter == 3){
            System.out.println("Failed to send string. Terminating!");
            return -1;
        }
        return 0;
    }

    public String receiveMessage(String hostname, int port){
        String message = receiveReliablePacket(hostname, port);

        if (!message.contains("Length:")){

            return null;
        }
        // Split the string by colon
        String[] parts = message.split(":");
        if (parts.length < 2) {
            return null;
        }
        int numberFragments = Integer.parseInt(parts[1].trim());

        String finalMessage = "";

        for(int i = 0; i < numberFragments; i++) {
            String splitMessage = receiveReliablePacket(hostname, port);
            finalMessage = String.join("", finalMessage, splitMessage);
        }

        return finalMessage;

    }

    private String receiveReliablePacket(String hostname, int port) {
        String received = null;
        try{
            InetAddress address = InetAddress.getByName(hostname);
            received = receivePacket();
            sendPacket(address,port,"ACK");

        }catch(SocketTimeoutException e){
            System.err.println("Timeout reached: " + e.getMessage());
        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }
        return received;
    }

    private void sendPacket(InetAddress address, int port, String message) throws IOException {
        sendData = message.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
        udpSocket.send(packet);
    }

    private String receivePacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        udpSocket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    /**
     * Closes the DatagramSocket
     */
    public void close() {
        udpSocket.close();
    }

    public static void main(String[] args) throws UnknownHostException {
        String hostname = null; String portString = null; String phrase = null; String keyword = null;

        try(BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))){

            System.out.print("Enter server name or IP address: ");
            hostname = stdin.readLine();
            System.out.print("Enter port: ");
            portString = stdin.readLine();
            System.out.print("Enter string: ");
            phrase = stdin.readLine();
            System.out.print("Enter keyword: ");
            keyword = stdin.readLine();

        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }

        //Input validation
        if(!inputDataValid(hostname,portString,phrase,keyword)) {
            System.err.println("Invalid input format. Terminating!");
            System.exit(1);
        }
        //Port validation
        int port = parseInt(portString);
        if(port < 1024 || port > 49151){
            System.err.println("Invalid port number. Terminating!");
            System.exit(1);
        }

        UDPClient client = new UDPClient(1000);

        InetAddress address = InetAddress.getByName(hostname);

        if(client.sendMessage(phrase,address,port) == -1){
            client.close();
            System.exit(1);
        }

        if(client.sendMessage(keyword,address,port) == -1) {
            client.close();
            System.exit(1);
        }
        String received = client.receiveMessage(hostname,port);
        if(received == null){
            client.close();
            System.exit(1);
        }
        System.out.println(received);
        received = client.receiveMessage(hostname,port);
        if(received == null){
            client.close();
            System.exit(1);
        }
        int repeat = Integer.parseInt(received);
        for(int i = 0; i < repeat; i++){
            received = client.receiveMessage(hostname,port);
            if(received == null){
                client.close();
                System.exit(1);
            }
            System.out.println(received);
        }
        client.close();
    }

    private static boolean inputDataValid(String hostname,String port, String phrase, String keyword){
        return !hostname.isEmpty() && !port.isEmpty() && !phrase.isEmpty() && !keyword.isEmpty();
    }
}
