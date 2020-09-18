package es.upm.etsit.irrigation.socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Comunicaciones {
    public static String[] consultarAlServidor(String mensajeEnviar, int numeroDeRespuestas){
        for(int j1 = 0; j1 < 3; j1 = j1 + 1){
            String[] respuestaDelServidor = Comunicaciones.consultarAlServidorProfundo(mensajeEnviar, numeroDeRespuestas);
            if(respuestaDelServidor != null){
                return respuestaDelServidor;
            }
        }
        return null;
    }
    private static String[] consultarAlServidorProfundo(String mensajeEnviar, int numeroDeRespuestas){
        Socket socket = null;
        DataOutputStream out;
        DataInputStream in;
        String[] salida = null;
        try{
            socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getByName("telecobets.ddns.net"), 4320), 5000);
            socket.setSoTimeout(10000);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(512);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            byte[] bytesClavePublica = publicKey.getEncoded();
            out.writeInt(bytesClavePublica.length);
            out.write(bytesClavePublica);
            int longitudMensaje = in.readInt();
            byte[] bytesRecibidos = new byte[longitudMensaje];
            in.read(bytesRecibidos);
            Cipher rsa;
            rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] bytesDesencriptados = rsa.doFinal(bytesRecibidos);
            Key key = new SecretKeySpec(bytesDesencriptados, 0, bytesDesencriptados.length, "AES");
            Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aes.init(Cipher.ENCRYPT_MODE, key);
            Comunicaciones.enviarMensaje(mensajeEnviar, out, aes);
            aes.init(Cipher.DECRYPT_MODE, key);
            salida = new String[numeroDeRespuestas];
            for(int j1 = 0; j1 < numeroDeRespuestas; j1 = j1 + 1){
                salida[j1] = Comunicaciones.recibirMensaje(in, aes);
            }
            return salida;
        }catch(Exception e){
            if((salida == null) || (salida[0] == null)){
                return null;
            }
            else{
                return salida;
            }
        }
        finally{
            try {
                socket.close();
            } catch (Exception e2){}
        }
    }
    private static void enviarMensaje(String mensaje, DataOutputStream streamSalida, Cipher aes) throws Exception{
        byte[] mensajeEncriptado = aes.doFinal(mensaje.getBytes());
        streamSalida.writeInt(mensajeEncriptado.length);
        streamSalida.write(mensajeEncriptado);
    }
    private static String recibirMensaje(DataInputStream streamEntrada, Cipher aes) throws Exception{
        int longitudMensajeEntrada = streamEntrada.readInt();
        byte[] bytesEntrada = new byte[longitudMensajeEntrada];
        streamEntrada.readFully(bytesEntrada);
        byte[] bytesDesencriptados = aes.doFinal(bytesEntrada);
        return new String(bytesDesencriptados);
    }
}