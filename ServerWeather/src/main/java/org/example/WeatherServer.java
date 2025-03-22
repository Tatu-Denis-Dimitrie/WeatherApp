package org.example;

import org.json.*;
import java.nio.file.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;

public class WeatherServer {
    private static List<Location> locations = new ArrayList<>();
    private static String WEATHER_DATA_FILE = "C:\\Users\\CNAE\\Downloads\\ServerWeather\\src\\main\\resources\\weather_data.json"; // completeaza cu calea catre fisier
    private static final String ADMIN_PASSWORD = "cucubau";

    public static void main(String[] args) throws IOException {
        loadWeatherData(WEATHER_DATA_FILE);
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Conexiune reusita cu baza de date");
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Serverul e pornit pe portul 12345");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client conectat de la: " + clientSocket.getInetAddress());
                handleClient(clientSocket, connection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadWeatherData(String fileName) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(fileName)));
        JSONObject json = new JSONObject(content);

        JSONArray locationsArray = json.getJSONArray("locations");
        for (int i = 0; i < locationsArray.length(); i++) {
            JSONObject locObj = locationsArray.getJSONObject(i);
            String name = locObj.getString("name");
            double latitude = locObj.getDouble("latitude");
            double longitude = locObj.getDouble("longitude");

            List<Forecast> forecastList = new ArrayList<>();
            JSONArray weatherArray = locObj.getJSONArray("weather");
            for (int j = 0; j < weatherArray.length(); j++) {
                JSONObject forecastObj = weatherArray.getJSONObject(j);
                forecastList.add(new Forecast(
                        forecastObj.getString("date"),
                        forecastObj.getString("condition"),
                        forecastObj.getDouble("temperature")
                ));
            }

            locations.add(new Location(name, latitude, longitude, forecastList));
        }
    }

    private static void handleClient(Socket clientSocket, Connection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String requestedLocation;

        while ((requestedLocation = in.readLine()) != null) {
            System.out.println("Clientul a cerut: " + requestedLocation);

            if ("exit".equalsIgnoreCase(requestedLocation)) {
                out.println("Serverul se inchide...");
                try {
                    connection.close();
                    System.out.println("Conexiunea cu baza de date a fost inchisa");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }

            if ("admin".equalsIgnoreCase(requestedLocation)) {
                String password = in.readLine();
                if (ADMIN_PASSWORD.equals(password)) {
                    out.println("ACCEPTED");
                    handleAdminCommands(in, out, connection);
                } else {
                    out.println("REJECTED");
                }
            } else if (requestedLocation.startsWith("coords:")) {
                handleCoordinatesRequest(requestedLocation, out, connection);
            } else {
                handleLocationRequest(requestedLocation, out, connection);
            }
        }

        clientSocket.close();
        System.out.println("Client deconectat");
    }

    private static void handleAdminCommands(BufferedReader in, PrintWriter out, Connection connection) throws IOException {
        String command;
        while ((command = in.readLine()) != null) {
            if ("exitadmin".equalsIgnoreCase(command)) {
                break;
            } else if ("adaugare".equalsIgnoreCase(command)) {
                addLocation(in, out, connection);
            } else if ("actualizare".equalsIgnoreCase(command)) {
                updateLocation(in, out, connection);
            } else if ("adaugaprognoza".equalsIgnoreCase(command)) {
                addForecast(in, out, connection);
            } else if ("incarcarejson".equalsIgnoreCase((command))){
                loadWeatherDataToDatabase(connection,out);
            }
        }
    }

    private static void handleLocationRequest(String locationName, PrintWriter out, Connection connection) {
        String query = """
        SELECT l.name, w.date, w.condition, w.temperature
        FROM locations l
        LEFT JOIN forecasts w ON l.id = w.location_id
        WHERE LOWER(l.name) = LOWER(?)
        ORDER BY w.date ASC
    """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationName);

            ResultSet rs = stmt.executeQuery();
            List<Forecast> forecasts = new ArrayList<>();

            while (rs.next()) {
                String date = rs.getString("date");
                String condition = rs.getString("condition");
                double temperature = rs.getDouble("temperature");

                Forecast forecast = new Forecast(date, condition, temperature);
                forecasts.add(forecast);
            }

            Optional<List<Forecast>> optionalForecasts = Optional.ofNullable(forecasts.isEmpty() ? null : forecasts);

            optionalForecasts.ifPresentOrElse(
                    forecastList -> {
                        Location location = new Location(locationName, 0.0, 0.0, forecastList);
                        out.println(location.getCurrentDayWeatherAndNext3Days());
                    },
                    () -> out.println("Locatie necunoscuta sau nu exista prognoze")
            );
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("Eroare la procesarea prognozelor");
        }
        out.println("END");
    }

    private static void addLocation(BufferedReader in, PrintWriter out, Connection connection) throws IOException {
        try {
            String destination = in.readLine().trim().toLowerCase();

            String name = in.readLine();
            double latitude = Double.parseDouble(in.readLine());
            double longitude = Double.parseDouble(in.readLine());

            if ("db".equals(destination)) {
                String query = "INSERT INTO locations (name, latitude, longitude) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, name);
                    stmt.setDouble(2, latitude);
                    stmt.setDouble(3, longitude);
                    stmt.executeUpdate();
                }
                out.println("Locatie adaugata cu succes in baza de date");
            } else if ("json".equals(destination)) {
                addLocationToJson(name, latitude, longitude);
                out.println("Locatie adaugata cu succes in fisierul JSON");
            } else {
                out.println("Destinatie invalida. incercati din nou");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("Eroare la adaugarea locatiei");
        }
        out.println("END");
    }



    private static void addLocationToJson(String name, double latitude, double longitude) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(WEATHER_DATA_FILE)));
        JSONObject json = new JSONObject(content);

        JSONArray locationsArray = json.getJSONArray("locations");
        JSONObject newLocation = new JSONObject();
        newLocation.put("name", name);
        newLocation.put("latitude", latitude);
        newLocation.put("longitude", longitude);
        newLocation.put("weather", new JSONArray());

        locationsArray.put(newLocation);

        try (FileWriter file = new FileWriter(WEATHER_DATA_FILE)) {
            file.write(json.toString(4));
        }
    }

    private static void updateLocation(BufferedReader in, PrintWriter out, Connection connection) throws IOException {
        try {
            String destination = in.readLine().trim().toLowerCase();
            String name = in.readLine();
            double latitude = Double.parseDouble(in.readLine());
            double longitude = Double.parseDouble(in.readLine());

            if ("db".equals(destination)) {
                String query = "UPDATE locations SET latitude = ?, longitude = ? WHERE LOWER(name) = LOWER(?)";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setDouble(1, latitude);
                    stmt.setDouble(2, longitude);
                    stmt.setString(3, name);
                    int rowsUpdated = stmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        out.println("Coordonatele locatiei au fost actualizate cu succes in baza de date.");
                    } else {
                        out.println("Locatia nu a fost gasita in baza de date.");
                    }
                }
            } else if ("json".equals(destination)) {
                boolean updated = updateLocationInJson(name, latitude, longitude);
                if (updated) {
                    out.println("Coordonatele locatiei au fost actualizate cu succes in fisierul JSON.");
                } else {
                    out.println("Locatia nu a fost gasita in fisierul JSON.");
                }
            } else {
                out.println("Destinatie invalida. incercati din nou.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("Eroare la actualizarea coordonatelor locatiei.");
        }
        out.println("END");
    }
    private static boolean updateLocationInJson(String name, double latitude, double longitude) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(WEATHER_DATA_FILE)));
        JSONObject json = new JSONObject(content);

        JSONArray locationsArray = json.getJSONArray("locations");
        for (int i = 0; i < locationsArray.length(); i++) {
            JSONObject locObj = locationsArray.getJSONObject(i);
            if (locObj.getString("name").equalsIgnoreCase(name)) {
                locObj.put("latitude", latitude);
                locObj.put("longitude", longitude);

                try (FileWriter file = new FileWriter(WEATHER_DATA_FILE)) {
                    file.write(json.toString(4));
                }
                return true;
            }
        }
        return false;
    }

    private static void addForecast(BufferedReader in, PrintWriter out, Connection connection) throws IOException {
        try {
            String destination = in.readLine().trim().toLowerCase();
            String name = in.readLine();
            String date = in.readLine();
            String condition = in.readLine();
            double temperature = Double.parseDouble(in.readLine());

            if ("db".equals(destination)) {
                String query = """
                INSERT INTO forecasts (location_id, date, condition, temperature)
                VALUES ((SELECT id FROM locations WHERE LOWER(name) = LOWER(?)), CAST(? AS DATE), ?, ?)
            """;
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, name);
                    stmt.setString(2, date);
                    stmt.setString(3, condition);
                    stmt.setDouble(4, temperature);
                    int rowsInserted = stmt.executeUpdate();
                    if (rowsInserted > 0) {
                        out.println("Prognoza adaugata cu succes in baza de date");
                    } else {
                        out.println("Locatia nu a fost gasita");
                    }
                }
            } else if ("json".equals(destination)) {
                addForecastToJson(name, date, condition, temperature);
                out.println("Prognoza adaugata cu succes in fisierul JSON");
            } else {
                out.println("Destinatie invalida. incercati din nou");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("Eroare la adaugarea prognozei");
        }
        out.println("END");
    }

    private static void addForecastToJson(String name, String date, String condition, double temperature) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(WEATHER_DATA_FILE)));
        JSONObject json = new JSONObject(content);

        JSONArray locationsArray = json.getJSONArray("locations");
        for (int i = 0; i < locationsArray.length(); i++) {
            JSONObject locObj = locationsArray.getJSONObject(i);
            if (locObj.getString("name").equalsIgnoreCase(name)) {
                JSONArray weatherArray = locObj.getJSONArray("weather");
                JSONObject newForecast = new JSONObject();
                newForecast.put("date", date);
                newForecast.put("condition", condition);
                newForecast.put("temperature", temperature);
                weatherArray.put(newForecast);

                try (FileWriter file = new FileWriter(WEATHER_DATA_FILE)) {
                    file.write(json.toString(4));
                }
                return;
            }
        }
        throw new IOException("Locatia nu a fost gasita in fisierul JSON");
    }

    private static void handleCoordinatesRequest(String request, PrintWriter out, Connection connection) {
        try {
            if (!request.startsWith("coords:")) {
                out.println("Format invalid. Trimiteti coordonatele sub forma 'coords:latitudine,longitudine'");
                return;
            }

            String[] parts = request.substring(7).split(",");

            if (parts.length < 2) {
                out.println("Format invalid. Trimiteti coordonatele sub forma 'coords:latitudine,longitudine'");
                return;
            }

            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());

            double maxDistance = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 50;

            String query = """
            SELECT name, latitude, longitude,
                   (6371 * acos(cos(radians(?)) * cos(radians(latitude)) * 
                   cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))) 
                   AS distance
            FROM locations
            WHERE (6371 * acos(cos(radians(?)) * cos(radians(latitude)) * 
                   cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))) 
                   <= ?
            ORDER BY distance
            LIMIT 1
        """;

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setDouble(1, latitude);
                stmt.setDouble(2, longitude);
                stmt.setDouble(3, latitude);
                stmt.setDouble(4, latitude);
                stmt.setDouble(5, longitude);
                stmt.setDouble(6, latitude);
                stmt.setDouble(7, maxDistance);

                ResultSet rs = stmt.executeQuery();

                Optional<String> locationName = Optional.empty();
                if (rs.next()) {
                    locationName = Optional.of(rs.getString("name"));
                }

                locationName.ifPresentOrElse(
                        name -> {
                            out.println("Cel mai apropiat oras: " + name);
                            handleLocationRequest(name, out, connection);
                        },
                        () -> out.println("Nu am gasit o locatie in raza de " + maxDistance + " km")
                );
            }
        } catch (Exception e) {
            out.println("Eroare la procesarea cererii: " + e.getMessage());
        }
    }


    private static void loadWeatherDataToDatabase(Connection connection,PrintWriter out) {
        try {
            for (Location location : locations) {
                String insertLocationQuery = "INSERT INTO locations (name, latitude, longitude) VALUES (?, ?, ?) ON CONFLICT (name) DO NOTHING";
                try (PreparedStatement stmt = connection.prepareStatement(insertLocationQuery, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, location.getName());
                    stmt.setDouble(2, location.getLatitude());
                    stmt.setDouble(3, location.getLongitude());
                    stmt.executeUpdate();

                    ResultSet keys = stmt.getGeneratedKeys();
                    int locationId = -1;
                    if (keys.next()) {
                        locationId = keys.getInt(1);
                    } else {
                        String selectQuery = "SELECT id FROM locations WHERE name = ?";
                        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                            selectStmt.setString(1, location.getName());
                            ResultSet rs = selectStmt.executeQuery();
                            if (rs.next()) {
                                locationId = rs.getInt("id");
                            }
                        }
                    }

                    if (locationId != -1) {
                        String insertForecastQuery = """
                        INSERT INTO forecasts (location_id, date, condition, temperature)
                        VALUES ((SELECT id FROM locations WHERE LOWER(name) = LOWER(?)), CAST(? AS DATE), ?, ?)
                    """;
                        try (PreparedStatement forecastStmt = connection.prepareStatement(insertForecastQuery)) {
                            for (Forecast forecast : location.getForecast()) {
                                forecastStmt.setString(1,location.getName());
                                forecastStmt.setString(2, forecast.getDate());
                                forecastStmt.setString(3, forecast.getWeather());
                                forecastStmt.setDouble(4, forecast.getTemperature());

                                forecastStmt.addBatch();
                            }
                            forecastStmt.executeBatch();
                        }

                    }
                }
            }
            out.println("Datele au fost incarcate cu succes in baza de date");
            System.out.println("Datele au fost incarcate cu succes in baza de date");
            out.println("END");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
