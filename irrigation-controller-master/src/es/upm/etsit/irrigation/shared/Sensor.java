package es.upm.etsit.irrigation.shared;

import java.io.Serializable;

public class Sensor implements Serializable {

  private static final long serialVersionUID = 3L;
  
  private final int ID;
  private transient double humidity;
  private transient double temperature;
  private transient double groundHumidity;
  
  
  public Sensor (int _ID) {
    ID = _ID;
    humidity = 0;
    temperature = 0;
    groundHumidity = 0;
  }

  public int getID() {
    return ID;
  }

  public double getHumidity() {
    return humidity;
  }


  public void setHumidity(double humidity) {
    this.humidity = humidity;
  }


  public double getTemperature() {
    return temperature;
  }


  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }


  public double getGroundHumidity() {
    return groundHumidity;
  }


  public void setGroundHumidity(double groundHumidity) {
    this.groundHumidity = groundHumidity;
  }
}
