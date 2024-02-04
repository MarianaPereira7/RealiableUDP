package org.example;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;

public class UDPServer {
    private DatagramSocket udpSocket;
    private byte[] receiveData = new byte[bufferLength];
    private byte[] sendData = new byte[bufferLength];
    private static int bufferLength = 20;
    private HashMap<String,String> clientMap = new HashMap<>();

    /**
     * Starts the EchoServer, binding it to the specified port
     * @param port UDP port to run the server
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

    /**
     * Waits for client data (client name, port number and message).
     * On a first client attempt: Saves the message in a map, to map all client's information.
     * On a second client attempt: Receives a keyword and anonymizes the message stored for the corresponding client.
     * 1) Successful case: Handles the message to be anonymized and sends it back, followed by the message "Socket Programming"
     * as many times as the chosen keyword is present in the original message.
     * 2) Unsuccessful case: At any point, if server does not receive an acknowledgment after trying to send the same
     * message for 3 consecutive times, it will print "Result transmission failed.Terminating!".
     */
    public void waitPackets(){
        while (true) {
            try{
                String[] received = receiveMessage();
                if(received == null) {
                    System.out.println("Did not receive valid string from client. Terminating!");
                    break;
                }

                String hostname        = received[0];
                int remotePort         = Integer.parseInt(received[1]);
                String message         = received[2];
                InetAddress remoteAddr = InetAddress.getByName(hostname);

                String client = remoteAddr.toString() + " - " + remotePort;

                //First registers the client in the map
                if(clientMap.get(client) == null){
                    clientMap.put(client,message);
                }else{
                    //When we already have a client, it means the phrase is already registered.
                    //In this case, keyword is received and sent along with the phrase to be anonymized.
                    String[] data = stringAnonymizer(clientMap.get(client),message);
                    boolean successfulDeliver = true;
                    for(String singleString : data){
                        if(sendMessage(singleString,remoteAddr,remotePort) == -1) {
                            System.out.println("Result transmission failed.Terminating!");
                            successfulDeliver = false;
                            break;
                        }
                    }
                    if(successfulDeliver){
                        int repetitions = Integer.parseInt(data[1]);
                        for(int i = 0; i < repetitions; i++) {
                            if (sendMessage("Socket Programming", remoteAddr, remotePort) == -1) {
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

    private String[] receiveMessage(){
        String[] received = receiveReliablePacket();
        String message = received[2];

        if (!message.contains("Packets: ")){
            return null;
        }
        // Split the string by colon
        String[] parts = message.split(":");
        if (parts.length < 2){
            return null;
        }
        int numberFragments = Integer.parseInt(parts[1].trim());

        String finalMessage = "";

        for(int i = 0; i < numberFragments; i++) {
            String splitMessage = receiveReliablePacket()[2];
            finalMessage = String.join("", finalMessage, splitMessage);
        }
        received[2] = finalMessage;
        return received;

    }

    private String[] receiveReliablePacket() {
        String[] received = new String[3];
        try{
            received = receivePacket();
            String hostname = received[0];
            int port = Integer.parseInt(received[1]);
            InetAddress address = InetAddress.getByName(hostname);
            sendPacket(address,port,"ACK");

        }catch(SocketTimeoutException e){
            System.err.println("Timeout reached: " + e.getMessage());
        }catch (IOException e){
            System.err.println("I/O error: " + e.getMessage());
        }
        return received;
    }

    private String[] receivePacket() throws IOException {
        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        udpSocket.receive(packet);
        String[] received = new String[3];
        received[0] = packet.getAddress().getHostAddress().toString();
        received[1] = Integer.toString(packet.getPort());
        received[2] = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    private int sendMessage(String message, InetAddress address, int port){
        //Calculate number of fragments, in case buffer length is lower than the message length
        int messageLength = message.length();
        int numberOfFragments = (int) Math.ceil((double) messageLength / bufferLength);
        String[] fragmentedMessage = divideMessage(message,numberOfFragments);

        if(sendReliablePacket("Packets:" + numberOfFragments,address,port) == -1){
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

    private int sendReliablePacket(String message, InetAddress address, int port) {
        int counter = 0;
        try{
            String received = "NONE";

            while (!received.equals("ACK") && counter < 3) {
                counter++;
                sendPacket(address,port,message);
                received = receivePacket()[2];
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

    private void sendPacket(InetAddress address, int port, String message) throws IOException {
        sendData = message.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
        udpSocket.send(packet);
    }

    private String lastCharEvaluator(String word){
        if(lastCharChecker(word)){
            char[] wordArray = new char[word.length()-1];
            for(int i = 0; i < word.length()-1; i++){
                wordArray[i] = word.charAt(i);
            }
            return String.valueOf(wordArray);
        }
        return word;

    }

    private boolean lastCharChecker(String word){
        char lastChar =  word.charAt(word.length()-1);
        return lastChar == ',' || lastChar == '.' || lastChar == '!' || lastChar == '?' || lastChar == ' ';
    }

    private String[] stringAnonymizer(String phrase, String wordToBeAnonymized){
        String[] anonymized = new String[2];
        int counter = 0;
        String[] phraseArray = phrase.split(" ");
        for(int i = 0; i < phraseArray.length; i++){
            if(lastCharEvaluator(phraseArray[i]).equalsIgnoreCase(wordToBeAnonymized)){
                phraseArray[i] = wordAnonimyzer(phraseArray[i]);
                counter++;
            }
        }
        anonymized[0] = String.join(" ",phraseArray);
        anonymized[1] = Integer.toString(counter);

        return anonymized;

    }

    private String wordAnonimyzer(String word){
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
