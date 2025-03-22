package org.example;

import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println(in.readLine());
            out.println(userInput.readLine());

            while (true) {
                String response = in.readLine();
                if (response == null || response.contains("Deconectare")) {
                    System.out.println(response);
                    break;
                }
                System.out.println(response);
                out.println(userInput.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
