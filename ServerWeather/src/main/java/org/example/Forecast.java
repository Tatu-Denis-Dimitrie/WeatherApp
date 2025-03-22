package org.example;

class Forecast {
    String date;
    String weather;
    double temperature;

    public Forecast(String date, String weather, double temperature) {
        this.date = date;
        this.weather = weather;
        this.temperature = temperature;
    }
    public String getDate() {
        return date;
    }

    public String getWeather() {
        return weather;
    }

    public Double getTemperature() {
        return temperature;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Forecast forecast = (Forecast) obj;
        return Double.compare(forecast.temperature, temperature) == 0 &&
                date.equals(forecast.date) &&
                weather.equals(forecast.weather);
    }
}