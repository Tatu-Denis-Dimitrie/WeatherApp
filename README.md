<h1 align="center">Weather Application in Java with Client-Server and PostgreSQL</h1>  
<h3 align="center">  
This Java project is a client-server weather application that retrieves and displays current weather conditions and a 3-day forecast. The client sends a location to the server, which queries a **PostgreSQL** database for the relevant weather data. If the exact location is not found, the server returns the closest available match based on latitude and longitude.  

**Features:**  
✔ Client-server architecture for real-time weather retrieval.  
✔ PostgreSQL database for efficient data storage.  
✔ Supports searching by city name with fallback to the nearest available location.  
✔ Admin functionality for updating weather data from a JSON file.  
✔ User-friendly command interface for displaying location and role after connection.  
</h3>

## File Structure
- **Server.java** - Contains the server-side logic for handling client requests and interacting with the PostgreSQL database.  
- **Client.java** - Contains the client-side logic for sending location queries to the server and displaying weather data.  
- **WeatherData.java** - Represents the weather data model.  
- **DatabaseUtil.java** - Utility class for connecting to and querying the PostgreSQL database.  
- **WeatherData.json** - Sample JSON file for populating the database with initial weather data.  

## How to Run

1. Open the project in **IntelliJ IDEA**.
2. Make sure you have **JDK 8 or higher** installed.
3. Import the project as a **Maven** or **Gradle** project (depending on the build system you use).
4. Configure your **PostgreSQL** database:
   - Ensure you have a PostgreSQL database running.
   - Create a database and import the weather data from the provided JSON file.
5. Update the **database connection settings** in the application to match your PostgreSQL credentials.
6. Run the **Server** class to start the server.
7. Open a **new terminal** or **IntelliJ** window and run the **Client** class.
8. Enter the location when prompted, and the application will return the weather data for that location.

**Note**: If the location is not found, the server will return the closest available match based on latitude and longitude.

## Dependencies
- **Java 8 or higher**
- **PostgreSQL** database
- **Maven** or **Gradle** for dependency management

## Future Enhancements
- Improve the user interface to visualize weather data.
- Implement real-time weather updates using WebSocket or REST APIs.
- Add support for additional weather services and APIs.
