package zer0.wscamera;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketOptions;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

public class ServerController {

    public final static String ADDR = "http://192.168.1.161:3000";

    MainActivity activity;
    Socket socket;

    public ServerController(final MainActivity activity){
        this.activity = activity;
        try{
            socket = IO.socket(ADDR);
            socket.connect();
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    activity.hideLoading();
                }
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    for(int i=0;i<args.length;i++){
                        Log.d("KAL",args[i].toString());
                    }
                }
            });
            socket.on("canClose", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.close();
                }
            });
        } catch (URISyntaxException e){
            e.printStackTrace();
        }
    }

    public void sendImage(byte[] bytes){
            socket.emit("newImage",bytes);
    }
}
