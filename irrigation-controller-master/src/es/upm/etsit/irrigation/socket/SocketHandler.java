package es.upm.etsit.irrigation.socket;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.upm.etsit.irrigation.Controller;
import es.upm.etsit.irrigation.shared.Controlador;

public class SocketHandler {
  private static final Logger logger = LogManager.getLogger(SocketHandler.class.getName());

  
  public static Controlador askController(String identificador) {
    
    String[] array = Comunicaciones.consultarAlServidor("/AskCo/" + identificador + "/", 1);
    String respuesta = null;
    
    if (array != null)
     respuesta = array[0];
    
    if(respuesta != null) {
    	if(respuesta.equals("IdFalso")) {
    		return null;
    	}
    	
    	byte[] modoSerializado = DatatypeConverter.parseHexBinary(respuesta);
    	
      try {
        ByteArrayInputStream dataInputStream = new ByteArrayInputStream(modoSerializado);
        ObjectInputStream is = new ObjectInputStream(dataInputStream);
        Controlador controller = (Controlador) is.readObject();
        
        return controller;
      } catch (IOException e) {
        logger.throwing(e);
      } catch (ClassNotFoundException e) {
        logger.throwing(e);
      }
      
    	return null;
    }
    
    return null;
  }
  
  public static Integer[] shouldIrrigateNow(String identificador) {
    String respuesta = Comunicaciones.consultarAlServidor("/ShReg/" + identificador + "/", 1)[0];
    if(respuesta != null){
    	if(respuesta.equals("IdFalso")){
    		return new Integer[0];
    	}
    	Integer[] salida = new Integer[Controller.MAX_ZONES];
    	String[] separados = respuesta.substring(1, respuesta.length() - 1).split("/");
    	for(int j1 = 0; j1 < Controller.MAX_ZONES; j1 = j1 + 1){
    		if(separados[j1].equals("null") == false){
    			salida[j1] = Integer.parseInt(separados[j1]);
    		}
    		else{
    			salida[j1] = 0;
    		}
    	}
    	return salida;
    }
    return null;
  }
  
  public static void sendPortStatus(String identificador, Boolean[] status) {
    String peticion = "/InfRg/" + identificador + "/";
    for(int j1 = 0; j1 < status.length; j1 = j1 + 1) {
    	if(status[j1] != null){
    		peticion = peticion + Boolean.toString(status[j1]) + "/";
    	}
    	else{
    		peticion = peticion + "null/";
    	}
    }
    Comunicaciones.consultarAlServidor(peticion, 0);
  }
}
