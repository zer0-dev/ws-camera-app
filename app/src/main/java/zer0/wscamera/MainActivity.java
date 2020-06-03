package zer0.wscamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    CameraController con;
    ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        con = new CameraController(this);
        con.initBackDevice();

        Button captureBtn = findViewById(R.id.captureBtn);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(con.isCamAvailable) con.capture();
            }
        });

        Button resetBtn = findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(con.isCamAvailable) con.createPreviewSession();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for(int i=0;i<grantResults.length;i++){
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                finish();
            } else{
                con.initBackDevice();
            }
        }
    }

    public void showLoading(){
        loading = ProgressDialog.show(this, "",
                "Loading...", true);
    }

    public void hideLoading(){
        loading.dismiss();
    }

    public Surface getSurface(){
        return ((SurfaceView) findViewById(R.id.surfaceView)).getHolder().getSurface();
    }
}
