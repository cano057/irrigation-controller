package es.upm.etsit.irrigation.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Mode implements Serializable {
  private static final long serialVersionUID = 3L;
  
  
  private int ID;
  private String name;
  private List<Zone> zones = new ArrayList<Zone>();
  
  
  public Mode(int _ID, String _name) {
    ID = _ID;
    name = _name;
  }
  
  public int getID() {
    return ID;
  }
  
  public void setID(int _ID) {
    ID = _ID;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String _name) {
    this.name = _name;
  }


  /**
   * @return the zones
   */
  public List<Zone> getZones() {
    return zones;
  }


  /**
   * @param zones the zones to set
   */
  public void setZones(List<Zone> zones) {
    this.zones = zones;
  }

  public void setZone(int nZone, Zone zone) {
        this.zones.set(nZone, zone);

  }

  public void addZone(Zone zone) {
    zones.add(zone);
  }
}
