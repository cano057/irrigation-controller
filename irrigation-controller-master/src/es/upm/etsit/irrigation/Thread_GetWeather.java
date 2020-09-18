package es.upm.etsit.irrigation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class Thread_GetWeather extends Thread {
  private final Logger logger = LogManager.getLogger(getClass().getName());
  
  private final int location;
  private final String WEATHER_ADDRESS = "https://opendata.aemet.es/opendata/api/prediccion/especifica/municipio/horaria/";
  private final String API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJxdWl0b3MubWNtQGdtYWlsLmNvbSIs"
      + "Imp0aSI6IjNkZDA0Y2Q4LTk4MmYtNGU5ZS04NTU1LTIwZGI2OTIyYjVkZiIsImlzcyI6IkFFTUVUIiwi"
      + "aWF0IjoxNTE5OTIyNDg3LCJ1c2VySWQiOiIzZGQwNGNkOC05ODJmLTRlOWUtODU1NS0yMGRiNjkyMmI1"
      + "ZGYiLCJyb2xlIjoiIn0.OJQ7Y9ywbaFGkYlbk9LEv8xllS_A0buf3-TXDIALpJI";
  
  
  public Thread_GetWeather(int _location) {
    location = _location;
  }
  
  public void run() {
    
    System.out.println("Location is " + location);
    
    if (location == -1)
      return;
    
    Calendar now = GregorianCalendar.getInstance();
    now.setTimeInMillis(System.currentTimeMillis());
    
    String dataURL = getURLOfData();
    
    JsonNode data = null;
    try {
      data = fetchURL(dataURL);
      
    } catch (IllegalArgumentException | UnirestException
        | InterruptedException e) {
      logger.throwing(e);
    }
    
    if (data != null) {
      JSONArray jsonArray = new JSONArray(data.toString());
      
      JSONArray dayArray = jsonArray.getJSONObject(0).getJSONObject("prediccion").getJSONArray("dia");
      
      JSONObject todayJSON = null;
      boolean isToday = false;
      int dayIterator = 0;
      while (!isToday && dayIterator < dayArray.length()) {
        todayJSON = dayArray.getJSONObject(dayIterator);
        
        String date = todayJSON.getString("fecha");
        DateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar todayCal = GregorianCalendar.getInstance();
        try {
          todayCal.setTime(sourceFormat.parse(date));
        } catch (ParseException e) {
          logger.throwing(e);
        }
        
        if (todayCal.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
          isToday = true;
        } else
          dayIterator++;
      }
      
      if (!isToday) {
        logger.error("Couldn't get today weather forecast");
        return;
      }
      
      
      int currentHour = now.get(Calendar.HOUR_OF_DAY);
      
      double[] amountOfPrecipitationNextHours = getAmountOfPrecipitationNextHours(todayJSON, currentHour);
      double chanceOfPrecipitationNextHours = getChanceOfPrecipitationNextHours(todayJSON, currentHour);
      double currentTemperature = getCurrentTemperature(todayJSON, currentHour);
      double currentRelativeHumidity = getCurrentRelativeHumidity(todayJSON, currentHour);
      
      Weather.setAmountOfPrecipitationNextHours(amountOfPrecipitationNextHours);
      Weather.setChanceOfPrecipitationNextHours(chanceOfPrecipitationNextHours);
      Weather.setCurrentTemperature(currentTemperature);
      Weather.setRelativeHumidity(currentRelativeHumidity);

      
      logger.debug("Url of data " + dataURL);
      logger.debug("Temperatura: " + currentTemperature);
      logger.debug("Humedad Relativa: " + currentRelativeHumidity);
      logger.debug("Humedad probPrecipitacion: " + chanceOfPrecipitationNextHours);
      for (int i = 0; i < amountOfPrecipitationNextHours.length; i++)
        logger.debug("Precipitacion proximas horas [" + i + "]: " + amountOfPrecipitationNextHours[i]);
    }
  }
  
  private String getURLOfData() {
    try {
      JsonNode data = fetchURL(WEATHER_ADDRESS + location + "/?api_key=" + API_KEY);
      
      if (data != null) {
        JSONArray response = data.getArray();
        
        int status = response.getJSONObject(0).getInt("estado");
        
        if (status == 200) {
          return response.getJSONObject(0).getString("datos");
        }
      }
      
    } catch (IllegalArgumentException | UnirestException | InterruptedException e) {
      logger.error("Couldn't fetch URL", e);
    }
    
    return null;
  }
  
  
  private JsonNode fetchURL(String url) throws UnirestException, InterruptedException,
    IllegalArgumentException {
    
    HttpResponse<JsonNode> response = Unirest.get(url)
        .header("cache-control", "no-cache")
        .asJson();
    
    String retryHeader = response.getHeaders().getFirst("Retry-After");

    if (response.getStatus() == 200) {
      return response.getBody();
    } else if (response.getStatus() == 429 && retryHeader != null) {
      Long waitSeconds = Long.valueOf(retryHeader);
      Thread.sleep(waitSeconds * 1000);
      return fetchURL(url);
    } else {
      throw new IllegalArgumentException("No data at " + url);
    }
  }
  
  private double[] getAmountOfPrecipitationNextHours(JSONObject todayJSON, int currentHour) {
    JSONArray precipitationArray = todayJSON.getJSONArray("precipitacion");
    double[] amountOfPrecipitationNextHours = new double[3];     
    int precipitationIt = 0;
    for (int i = 0; i < precipitationArray.length(); i++) {
      JSONObject precipitationInThisHourObject = precipitationArray.getJSONObject(i);
      
      int hourInJSON = precipitationInThisHourObject.getInt("periodo");
      
      if (hourInJSON > currentHour) {
        amountOfPrecipitationNextHours[precipitationIt] = precipitationInThisHourObject.getDouble("value");
        precipitationIt++;
      }
      
      if (precipitationIt == amountOfPrecipitationNextHours.length)
        break;
    }
    
    return amountOfPrecipitationNextHours;
  }
  
  private double getChanceOfPrecipitationNextHours(JSONObject todayJSON, int currentHour) {
    JSONArray probPrecipitationArray = todayJSON.getJSONArray("probPrecipitacion");
    double chanceOfPrecipitationNextHours = 0;
    for (int i = 0; i < probPrecipitationArray.length(); i++) {
      JSONObject probPrecipitationInThisHourObject = probPrecipitationArray.getJSONObject(i);
      
      // It comes like: 0208 this is between 2am and 8am so we need to split it
      String periodInJSON = probPrecipitationInThisHourObject.getString("periodo");
      
      String firstHour = periodInJSON.substring(0, 2);
      String lastHour = periodInJSON.substring(2);
      
      if (Integer.parseInt(firstHour) < currentHour && Integer.parseInt(lastHour) > currentHour) {
        chanceOfPrecipitationNextHours = probPrecipitationInThisHourObject.getDouble("value");
        break;
      }
    }
    
    return chanceOfPrecipitationNextHours;
  }
  
  private double getCurrentTemperature(JSONObject todayJSON, int currentHour) {
    JSONArray temperatureArray = todayJSON.getJSONArray("temperatura");
    double currentTemperature = 0;
    for (int i = 0; i < temperatureArray.length(); i++) {
      JSONObject temperatureObject = temperatureArray.getJSONObject(i);
      
      int hourInJSON = temperatureObject.getInt("periodo");
      
      
      if (hourInJSON == currentHour) {
        currentTemperature = temperatureObject.getDouble("value");
        break;
      }
    }
    
    return currentTemperature;
  }
  
  private double getCurrentRelativeHumidity(JSONObject todayJSON, int currentHour) {
    JSONArray humidityArray = todayJSON.getJSONArray("humedadRelativa");
    double currentRelativeHumidity = 0;
    for (int i = 0; i < humidityArray.length(); i++) {
      JSONObject humidityObject = humidityArray.getJSONObject(i);
      
      int hourInJSON = humidityObject.getInt("periodo");
      
      
      if (hourInJSON == currentHour) {
        currentRelativeHumidity = humidityObject.getDouble("value");
        break;
      }
    }
    
    return currentRelativeHumidity;
  }
}
