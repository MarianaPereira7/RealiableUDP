package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

import static java.lang.Integer.parseInt;

public class UDPClient {
    private DatagramSocket udpSocket;
    private byte[] receiveData = new byte[256];
    private byte[] sendData = new byte[256];

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

    /**
     * Repeatedly reads a line from terminal, sends it to a server living at hostname:port, and waits for a reply
     * Use CTRL + D to exit
     *
     *  @param hostname Name of the UDP server
     *  @param port Port binded to the UDP server living at hostname
     */
    public int sendData(String message, String hostname, int port) {
        int counter = 0;
        try{
            InetAddress address = InetAddress.getByName(hostname);
            String received = "NONE";

            while (!received.equals("ACK") && counter < 3) {
                counter++;
                sendPacket(address,port,message);
                received = receivePacket();
                System.out.println(received); // TODO DELETE
            }

        }catch(SocketTimeoutException e){
            System.err.println("Timeout reached: " + e.getMessage());
        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }

        //Validação mensagem recebida com sucesso.
        if(counter == 3){
            System.out.println("Failed to send string. Terminating!");
            return -1;
        }
        return 0;
    }

    public String receiveData(String hostname, int port) {
        String received = null;
        try{
            InetAddress address = InetAddress.getByName(hostname);
            received = receivePacket();
            // System.out.println(received); // TODO DELETE
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

    public static void main(String[] args) {
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
        int port = parseInt(portString);
        if(!inputDataValid(hostname,portString,phrase,keyword)) {
            System.err.println("Invalid input format. Terminating!");
            System.exit(1);
        }
        //Port validation
        if(port < 1024 || port > 49151){
            System.err.println("Invalid port number. Terminating!");
            System.exit(1);
        }

        UDPClient client = new UDPClient(1000);

        if(client.sendData(phrase,hostname,port) == -1){
            client.close();
            System.exit(1);
        }

        if(client.sendData(keyword,hostname,port) == -1){
            client.close();
            System.exit(1);
        }

        System.out.println(client.receiveData(hostname,port));
        int repeate = Integer.parseInt(client.receiveData(hostname,port));
        for(int i = 0; i < repeate; i++){
            System.out.println(client.receiveData(hostname,port));
        }
        client.close();
    }

    private static boolean inputDataValid(String hostname,String port, String phrase, String keyword){
        return !hostname.isEmpty() && !port.isEmpty() && !phrase.isEmpty() && !keyword.isEmpty();
    }
}
