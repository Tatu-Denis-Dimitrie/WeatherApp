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

            // Citire linie de la server: mesajul de conectare
            String serverResponse = in.readLine();
            System.out.println(serverResponse);

            // Trimiterea numelui de utilizator
            out.println(userInput.readLine());

            // Citire toate răspunsurile de la server
            readServerResponse(in);

            // Citire comandă de la utilizator și trimitere către server
            while (true) {
                String command = userInput.readLine();
                out.println(command);

                // Citire și afișare răspuns de la server după fiecare comandă
                readServerResponse(in);

                // Dacă serverul trimite mesajul "Deconectare", încheiem
                if (serverResponse.contains("Deconectare")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metoda care citește toate liniile de la server până la un semnal de terminare
    private static void readServerResponse(BufferedReader in) throws IOException {
        String line;
        StringBuilder completeResponse = new StringBuilder();
        while ((line = in.readLine()) != null) {
            if ("END".equals(line)) {  // Semnal de terminare
                break;
            }
            completeResponse.append(line).append("\n");
        }
        System.out.println(completeResponse.toString());
    }
}
