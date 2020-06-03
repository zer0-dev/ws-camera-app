package zer0.wscamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraController {

    MainActivity activity;
    CameraManager manager;
    CameraDevice backCam;
    CameraCaptureSession previewSession;
    ImageReader reader;
    Surface mainSurface;

    boolean isCamAvailable = false;

    public CameraController(MainActivity activity){
        this.activity = activity;
        if(activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            activity.requestPermissions(new String[]{Manifest.permission.CAMERA},1);
        }
    }

    public void initBackDevice(){
        manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try{
            String[] cams = manager.getCameraIdList();
            for(int i=0;i<cams.length;i++){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cams[i]);
                if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK){
                    openCamera(cams[i]);
                }
            }
        } catch(CameraAccessException e){
            cameraFailed();
        }
    }

    public void openCamera(String camId){
        try{
            if(activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA},1);
                return;
            }
            manager.openCamera(camId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    backCam = camera;
                    mainSurface = activity.getSurface();
                    createPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraFailed();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraFailed();
                }
            },null);
        } catch (CameraAccessException e){
            cameraFailed();
        }
    }

    public void startPreview(CameraCaptureSession session, CaptureRequest request){
        try{
            session.setRepeatingRequest(request, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            },null);
        } catch (CameraAccessException e){
            cameraFailed();
        }
    }

    public void createPreviewSession(){
        reader = ImageReader.newInstance(1920,1080, ImageFormat.JPEG,1);
        reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d("KAL","AVAILABLE");
                Image img = reader.acquireNextImage();
                ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                activity.showLoading();
                new ServerController(activity).sendImage(bytes);
            }
        },null);
        try{
            backCam.createCaptureSession(Arrays.asList(mainSurface,reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    previewSession = session;
                    isCamAvailable = true;
                    try{
                        CaptureRequest.Builder b = backCam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        b.addTarget(mainSurface);
                        startPreview(previewSession,b.build());
                    } catch (CameraAccessException e){
                        cameraFailed();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    cameraFailed();
                }
            },null);
        } catch (CameraAccessException e){
            cameraFailed();
        }
    }

    public void capture(){
        try{
            CaptureRequest.Builder b = backCam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            b.addTarget(reader.getSurface());
            b.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation(manager.getCameraCharacteristics(backCam.getId()),activity.getResources().getConfiguration().orientation));
            previewSession.abortCaptures();
            previewSession.capture(b.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Log.d("KAL","CAPTURED");
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    cameraFailed();
                }
            },null);
        } catch (CameraAccessException e){
            cameraFailed();
        }
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
        return jpegOrientation;
    }

    public void cameraFailed(){
        isCamAvailable = false;
        Log.d("KAL","camera failed to open");
    }

}
