package es.upm.etsit.irrigation;

import es.upm.etsit.irrigation.shared.Zone;

public class Weather {
  
  private static final int MAX_NEXT_HOURS = 3;
  
  private static double currentTemperature = 0;
  private static double currentRelativeHumidity = 0;
  private static double currentSoilHumidity = 0;
  private static double chanceOfPrecipitationNextHours = 0;
  private static double[] amountOfPrecipitationNextHours = new double[MAX_NEXT_HOURS];
  
  
  // TODO: Improve this. Make a proper algorithm
  public static synchronized boolean shouldIrrigateNow() {
    if (isSoilDry() || (currentRelativeHumidity < 50 && chanceOfPrecipitationNextHours < 0.7
        && amountOfPrecipitationNextHours[0] < 1
        && amountOfPrecipitationNextHours[1] < 1))
      return true;
    return false;
  }
  
  
  // TODO: Implement this
  public static synchronized boolean newToIrrigateNow(Zone zone) {
    return true;
  }
  
  public static synchronized double getCurrentTemperature() {
    return currentTemperature;
  }

  public static synchronized void setCurrentTemperature(double currentTemperature) {
    Weather.currentTemperature = currentTemperature;
  }
  
  public static synchronized double getCurrentRelativeHumidity() {
    return currentRelativeHumidity;
  }
  
  public static synchronized void setRelativeHumidity(double relHumidity) {
    Weather.currentRelativeHumidity = relHumidity;
  }
  
  public static synchronized double getCurrentGroundHumidity() {
    return currentSoilHumidity;
  }

  public static synchronized void setCurrentGroundHumidity(double currentGroundHumidity) {
    Weather.currentSoilHumidity = currentGroundHumidity;
  }

  public static synchronized double getChanceOfPrecipitationNextHours() {
    return chanceOfPrecipitationNextHours;
  }

  public static synchronized void setChanceOfPrecipitationNextHours(
      double chanceOfPrecipitationNextHours) {
    Weather.chanceOfPrecipitationNextHours = chanceOfPrecipitationNextHours;
  }

  public static synchronized double[] getAmountOfPrecipitationNextHours() {
    return amountOfPrecipitationNextHours;
  }

  public static synchronized void setAmountOfPrecipitationNextHours(
      double[] amountOfPrecipitationNextHours) {
    Weather.amountOfPrecipitationNextHours = amountOfPrecipitationNextHours;
  }
  
  
  // TODO: Make some research
  private static synchronized boolean isSoilDry() {
    return currentSoilHumidity < 200;
  }
  
}
