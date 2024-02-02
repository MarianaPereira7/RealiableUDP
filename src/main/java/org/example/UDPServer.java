package org.example;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;

public class UDPServer {
    private DatagramSocket udpSocket;
    private byte[] receiveData = new byte[256];
    private byte[] sendData = new byte[256];
    private HashMap<String,String> clientMap = new HashMap<>();

    /**
     * Starts the EchoServer, bindind it to the specified port
     *
     * @param port UDP port binded by the server
     */
    UDPServer(int port){
        udpSocket = null;

        try{
            udpSocket = new DatagramSocket(port);
            System.out.println("Created UDP socket at " + udpSocket.getLocalPort());
        }catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }

    }
    public int sendData(String message, InetAddress address, int port) {
        int counter = 0;
        try{
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
    /**
     * Waits for client data and sends back lines as they are typed
     */
    public void waitPackets(){
        DatagramPacket packet;
        InetAddress remoteAddr;
        int remotePort;
        String received;

        while (true) {
            try{
                packet = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(packet);
                remoteAddr = packet.getAddress();
                remotePort = packet.getPort();
                received = new String(packet.getData(), 0, packet.getLength());

                System.out.println("Received " + received.length() + " bytes from (" + remoteAddr.getHostAddress() + "," + remotePort +")"); // TODO DELETE

                sendPacket(remoteAddr,remotePort,"ACK");
                String client = remoteAddr.toString() + " - " + Integer.toString(remotePort);

                if(clientMap.get(client) == null){
                    clientMap.put(client,received);
                }else{
                    String[] data = stringAnonymizer(clientMap.get(client),received);
                    boolean successfulDeliver = true;
                    for(String singleString : data){
                        if(sendData(singleString,remoteAddr,remotePort) == -1) {
                            System.out.println("Result transmission failed.Terminating!");
                            successfulDeliver = false;
                            break;
                        }
                    }
                    if(successfulDeliver){
                        for(int i = 0; i < Integer.parseInt(data[1]); i++) {
                            if (sendData("Socket Programming", remoteAddr, remotePort) == -1) {
                                System.out.println("Result transmission failed.Terminating!");
                                break;
                            }
                        }
                    }
                    clientMap.remove(client);
                }

            } catch (IOException e) {
                System.err.println("I/O error: " + e.getMessage());
            }
        }
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

    public String lastCharEvaluator(String word){
        if(lastCharChecker(word)){
            char[] wordArray = new char[word.length()-1];
            for(int i = 0; i < word.length()-1; i++){
                wordArray[i] = word.charAt(i);
            }
            return String.valueOf(wordArray);
        }
        return word;

    }

    public boolean lastCharChecker(String word){
        char lastChar =  word.charAt(word.length()-1);
        return lastChar == ',' || lastChar == '.' || lastChar == '!' || lastChar == '?' || lastChar == ' ';
    }

    public String[] stringAnonymizer(String phrase, String wordToBeAnonimized){
        String[] annonymized = new String[2];
        int counter = 0;
        String[] phraseArray = phrase.split(" ");
        for(int i = 0; i < phraseArray.length; i++){
            if(lastCharEvaluator(phraseArray[i]).equalsIgnoreCase(wordToBeAnonimized)){
                phraseArray[i] = wordAnonimizer(phraseArray[i]);
                counter++;
            }
        }
        annonymized[0] = String.join(" ",phraseArray);
        annonymized[1] = Integer.toString(counter);

        return annonymized;

    }

    public String wordAnonimizer(String word){
        char[] wordArray = new char[word.length()];
        for(int i = 0; i < word.length(); i++){
            wordArray[i] = word.charAt(i);
        }
        if(lastCharChecker(word)){
            for(int i = 0; i < word.length()-1; i++){
                wordArray[i] = 'X';
            }
            return String.valueOf(wordArray);
        }
        Arrays.fill(wordArray, 'X');
        return String.valueOf(wordArray);
    }


    /**
     * Creates a UDP server and waits for client packets
     * @param args The server's port should be passed here
     **/
    public static void main(String[] args) {
        if (args.length < 1){
            System.err.println("Usage: java UDPEchoServer <port number>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        UDPServer echoServer = new UDPServer(port);

        echoServer.waitPackets();
    }
}
