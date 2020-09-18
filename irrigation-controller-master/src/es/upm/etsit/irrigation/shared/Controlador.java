package es.upm.etsit.irrigation.shared;

import java.util.List;
import java.io.Serializable;


/**
 * Created by cano0 on 10/03/2018.
 */

public class Controlador implements Serializable {
    private static final long serialVersionUID = 3L;

    private int version;
    private String titulo = "";
    private String id = "";
    private boolean tiempar = false;
    private String municipio;
    private Mode active_mode;
    private List<Mode> list_mode;

    public Controlador(String titulo, String id, boolean tiempar, String municipio, Mode active_mode, List<Mode> list_modo) {
        
        this.titulo = titulo;
        this.id = id;
        this.tiempar = tiempar;
        this.municipio = municipio;
        this.active_mode = active_mode;
        this.list_mode = list_modo;
        this.version = 0;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getId() {
        return id;
    }

    public Boolean getTiempar() { return tiempar; }

    public String getMunicipio() { return municipio; }

    public Mode getActiveMode() { return active_mode; }

    public List<Mode> getList_modo() {
        return list_mode;
    }

    public void setMode(Mode _mode) {
        for(int i = 0; i > list_mode.size(); i++) {
            if ( list_mode.get(i).getID()== _mode.getID()) {
                list_mode.set(i, _mode);
            }
        }
    }

    public void setActive_mode(Mode newActiveMode) {
        active_mode = newActiveMode;
    }

    public void addMode(Mode newMode) { list_mode.add(newMode); }
    
    
    public int getVersion() { return this.version; }
     
    
    public void update() { this.version++; }
    
}
