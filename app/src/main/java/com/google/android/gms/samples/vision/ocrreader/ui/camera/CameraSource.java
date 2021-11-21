package com.google.android.gms.samples.vision.ocrreader.ui.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.StringDef;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.io.IOException;
import java.lang.Thread.State;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Note: This requires Google Play Services 8.1 or higher, due to using indirect byte buffers for
// storing images.

@SuppressWarnings("deprecation")
public class CameraSource {
    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK;
    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT;

    private static final String TAG = "OpenCameraSource";

    private static final int DUMMY_TEXTURE_NAME = 100;

    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

    @StringDef({
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
            Camera.Parameters.FOCUS_MODE_AUTO,
            Camera.Parameters.FOCUS_MODE_EDOF,
            Camera.Parameters.FOCUS_MODE_FIXED,
            Camera.Parameters.FOCUS_MODE_INFINITY,
            Camera.Parameters.FOCUS_MODE_MACRO
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface FocusMode {}

    @StringDef({
            Camera.Parameters.FLASH_MODE_ON,
            Camera.Parameters.FLASH_MODE_OFF,
            Camera.Parameters.FLASH_MODE_AUTO,
            Camera.Parameters.FLASH_MODE_RED_EYE,
            Camera.Parameters.FLASH_MODE_TORCH
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface FlashMode {}

    private Context context;

    private final Object cameraLock = new Object();

    // Guarded by cameraLock
    private Camera camera;

    private int mFacing = CAMERA_FACING_BACK;

    /**
     * Rotation of the device, and thus the associated preview images captured from the device.
     */
    private int rotation;

    private Size previewSize;


    private float requestedFps = 30.0f;
    private int requestedPreviewWidth = 1024;
    private int requestedPreviewHeight = 768;


    private String focusMode = null;
    private String flashMode = null;

    private SurfaceView dummySurfaceView;
    private SurfaceTexture dummySurfaceTexture;

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread processingThread;
    private FrameProcessingRunnable frameProcessor;

    /**
     * Map to convert between a byte array, received from the camera, and its associated byte
     * buffer.  We use byte buffers internally because this is a more efficient way to call into
     * native code later (avoids a potential copy).
     */
    private Map<byte[], ByteBuffer> bytesToByteBuffer = new HashMap<>();



    // Builder for configuring and creating an associated camera source.
    public static class Builder {
        private final Detector<?> detector;
        private CameraSource cameraSource = new CameraSource();

        /**
         * Creates a camera source builder with the supplied context and detector.  Camera preview
         * images will be streamed to the associated detector upon starting the camera source.
         */
        public Builder(Context context, Detector<?> detector) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            if (detector == null) {
                throw new IllegalArgumentException("No detector supplied.");
            }

            this.detector = detector;
            cameraSource.context = context;
        }
        /**
         * Sets the requested frame rate in frames per second.  If the exact requested value is not
         * not available, the best matching available value is selected.   Default: 30.
         */
        public Builder setRequestedFps(float fps) {
            if (fps <= 0) {
                throw new IllegalArgumentException("Invalid fps: " + fps);
            }
            cameraSource.requestedFps = fps;
            return this;
        }

        public Builder setFocusMode(@FocusMode String mode) {
            cameraSource.focusMode = mode;
            return this;
        }

        public Builder setFlashMode(@FlashMode String mode) {
            cameraSource.flashMode = mode;
            return this;
        }

        /**
         * Sets the desired width and height of the camera frames in pixels. If the exact desired
         * values are not available options, the best matching available options are selected.
         */
        public Builder setRequestedPreviewSize(int width, int height) {
            final int MAX = 1000000;
            if ((width <= 0) || (width > MAX) || (height <= 0) || (height > MAX)) {
                throw new IllegalArgumentException("Invalid preview size: " + width + "x" + height);
            }
            cameraSource.requestedPreviewWidth = width;
            cameraSource.requestedPreviewHeight = height;
            return this;
        }

        /**
         * Sets the camera to use (either {@link #CAMERA_FACING_BACK} or
         * {@link #CAMERA_FACING_FRONT}). Default: back facing.
         */
        public Builder setFacing(int facing) {
            if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
                throw new IllegalArgumentException("Invalid camera: " + facing);
            }
            cameraSource.mFacing = facing;
            return this;
        }

        /**
         * Creates an instance of the camera source.
         */
        public CameraSource build() {
            cameraSource.frameProcessor = cameraSource.new FrameProcessingRunnable(detector);
            return cameraSource;
        }
    }

    /**
     * Callback interface used to signal the moment of actual image capture.
     */
    public interface ShutterCallback {
        void onShutter();
    }
    /**
     * Callback interface used to supply image data from a photo capture.
     */
    public interface PictureCallback {

        void onPictureTaken(byte[] data);
    }

    /**
     * Callback interface used to notify on completion of camera auto focus.
     */
    public interface AutoFocusCallback {

        void onAutoFocus(boolean success);
    }
    /**
     * Callback interface used to notify on auto focus start and stop.
     */
    public interface AutoFocusMoveCallback {

        void onAutoFocusMoving(boolean start);
    }
    /**
     * Stops the camera and releases the resources of the camera and underlying detector.
     */
    public void release() {
        synchronized (cameraLock) {
            stop();
            frameProcessor.release();
        }
    }
    /**
     * Opens the camera and starts sending preview frames to the underlying detector.  The preview
     * frames are not displayed.
     *
     * @throws IOException if the camera's preview texture or display could not be initialized
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public CameraSource start() throws IOException {
        synchronized (cameraLock) {
            if (camera != null) {
                return this;
            }

            camera = createCamera();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                dummySurfaceTexture = new SurfaceTexture(DUMMY_TEXTURE_NAME);
                camera.setPreviewTexture(dummySurfaceTexture);
            } else {
                dummySurfaceView = new SurfaceView(context);
                camera.setPreviewDisplay(dummySurfaceView.getHolder());
            }
            camera.startPreview();

            processingThread = new Thread(frameProcessor);
            frameProcessor.setActive(true);
            processingThread.start();
        }
        return this;
    }
    /**
     * Opens the camera and starts sending preview frames to the underlying detector.  The supplied
     * surface holder is used for the preview so frames can be displayed to the user.
     *
     * @param surfaceHolder the surface holder to use for the preview frames
     * @throws IOException if the supplied surface holder could not be used as the preview display
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public CameraSource start(SurfaceHolder surfaceHolder) throws IOException {
        synchronized (cameraLock) {
            if (camera != null) {
                return this;
            }

            camera = createCamera();
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

            processingThread = new Thread(frameProcessor);
            frameProcessor.setActive(true);
            processingThread.start();
        }
        return this;
    }
    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     */
    public void stop() {
        synchronized (cameraLock) {
            frameProcessor.setActive(false);
            if (processingThread != null) {
                try {
                    processingThread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Frame processing thread interrupted on release.");
                }
                processingThread = null;
            }
            // clear the buffer to prevent oom exceptions
            bytesToByteBuffer.clear();

            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallbackWithBuffer(null);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        camera.setPreviewTexture(null);

                    } else {
                        camera.setPreviewDisplay(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to clear camera preview: " + e);
                }
                camera.release();
                camera = null;
            }
        }
    }

    /**
     * Returns the preview size that is currently in use by the underlying camera.
     */
    public Size getPreviewSize() {
        return previewSize;
    }
    /**
     * Returns the selected camera; one of {@link #CAMERA_FACING_BACK} or
     */
    public int getCameraFacing() {
        return mFacing;
    }

    public int doZoom(float scale) {
        synchronized (cameraLock) {
            if (camera == null) {
                return 0;
            }
            int currentZoom = 0;
            int maxZoom;
            Camera.Parameters parameters = camera.getParameters();
            if (!parameters.isZoomSupported()) {
                Log.w(TAG, "Zoom is not supported on this device");
                return currentZoom;
            }
            maxZoom = parameters.getMaxZoom();

            currentZoom = parameters.getZoom() + 1;
            float newZoom;
            if (scale > 1) {
                newZoom = currentZoom + scale * (maxZoom / 10);
            } else {
                newZoom = currentZoom * scale;
            }
            currentZoom = Math.round(newZoom) - 1;
            if (currentZoom < 0) {
                currentZoom = 0;
            } else if (currentZoom > maxZoom) {
                currentZoom = maxZoom;
            }
            parameters.setZoom(currentZoom);
            camera.setParameters(parameters);
            return currentZoom;
        }
    }

    public void takePicture(ShutterCallback shutter, PictureCallback jpeg) {
        synchronized (cameraLock) {
            if (camera != null) {
                PictureStartCallback startCallback = new PictureStartCallback();
                startCallback.mDelegate = shutter;
                PictureDoneCallback doneCallback = new PictureDoneCallback();
                doneCallback.mDelegate = jpeg;
                camera.takePicture(startCallback, null, null, doneCallback);
            }
        }
    }

    /**
     * Gets the current focus mode setting.
     */
    @Nullable
    @FocusMode
    public String getFocusMode() {
        return focusMode;
    }
    /**
     * Sets the focus mode.
     *
     * @param mode the focus mode
     * @return {@code true} if the focus mode is set, {@code false} otherwise
     * @see #getFocusMode()
     */
    public boolean setFocusMode(@FocusMode String mode) {
        synchronized (cameraLock) {
            if (camera != null && mode != null) {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters.getSupportedFocusModes().contains(mode)) {
                    parameters.setFocusMode(mode);
                    camera.setParameters(parameters);
                    focusMode = mode;
                    return true;
                }
            }

            return false;
        }
    }
    /**
     * Gets the current flash mode setting.
     *
     * @return current flash mode. null if flash mode setting is not
     * supported or the camera is not yet created.
     */
    @Nullable
    @FlashMode
    public String getFlashMode() {
        return flashMode;
    }
    /**
     * Sets the flash mode.
     *
     * @param mode flash mode.
     * @return {@code true} if the flash mode is set, {@code false} otherwise
     * @see #getFlashMode()
     */
    public boolean setFlashMode(@FlashMode String mode) {
        synchronized (cameraLock) {
            if (camera != null && mode != null) {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters.getSupportedFlashModes().contains(mode)) {
                    parameters.setFlashMode(mode);
                    camera.setParameters(parameters);
                    flashMode = mode;
                    return true;
                }
            }

            return false;
        }
    }
    /**
     * Starts camera auto-focus and registers a callback function to run when
     * the camera is focused.
     * @param cb the callback to run
     */
    public void autoFocus(@Nullable AutoFocusCallback cb) {
        synchronized (cameraLock) {
            if (camera != null) {
                CameraAutoFocusCallback autoFocusCallback = null;
                if (cb != null) {
                    autoFocusCallback = new CameraAutoFocusCallback();
                    autoFocusCallback.mDelegate = cb;
                }
                camera.autoFocus(autoFocusCallback);
            }
        }
    }

    /**
     * Cancels any auto-focus function in progress.
     * Whether or not auto-focus is currently in progress,
     * this function will return the focus position to the default.
     * If the camera does not support auto-focus, this is a no-op.
     *
     * @see #autoFocus(AutoFocusCallback)
     */
    public void cancelAutoFocus() {
        synchronized (cameraLock) {
            if (camera != null) {
                camera.cancelAutoFocus();
            }
        }
    }
    /**
     * Sets camera auto-focus move callback.
     *
     * @param cb the callback to run
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean setAutoFocusMoveCallback(@Nullable AutoFocusMoveCallback cb) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false;
        }

        synchronized (cameraLock) {
            if (camera != null) {
                CameraAutoFocusMoveCallback autoFocusMoveCallback = null;
                if (cb != null) {
                    autoFocusMoveCallback = new CameraAutoFocusMoveCallback();
                    autoFocusMoveCallback.mDelegate = cb;
                }
                camera.setAutoFocusMoveCallback(autoFocusMoveCallback);
            }
        }

        return true;
    }


    private CameraSource() {
    }
    /**
     * Wraps the camera1 shutter callback so that the deprecated API isn't exposed.
     */
    private class PictureStartCallback implements Camera.ShutterCallback {
        private ShutterCallback mDelegate;

        @Override
        public void onShutter() {
            if (mDelegate != null) {
                mDelegate.onShutter();
            }
        }
    }
    /**
     * Wraps the final callback in the camera sequence, so that we can automatically turn the camera
     * preview back on after the picture has been taken.
     */
    private class PictureDoneCallback implements Camera.PictureCallback {
        private PictureCallback mDelegate;

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onPictureTaken(data);
            }
            synchronized (cameraLock) {
                if (CameraSource.this.camera != null) {
                    CameraSource.this.camera.startPreview();
                }
            }
        }
    }
    /**
     * Wraps the camera1 auto focus callback so that the deprecated API isn't exposed.
     */
    private class CameraAutoFocusCallback implements Camera.AutoFocusCallback {
        private AutoFocusCallback mDelegate;

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocus(success);
            }
        }
    }

    /**
     * Wraps the camera1 auto focus move callback so that the deprecated API isn't exposed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private class CameraAutoFocusMoveCallback implements Camera.AutoFocusMoveCallback {
        private AutoFocusMoveCallback mDelegate;

        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocusMoving(start);
            }
        }
    }
    /**
     * Opens the camera and applies the user settings.
     *
     * @throws RuntimeException if the method fails
     */
    @SuppressLint("InlinedApi")
    private Camera createCamera() {
        int requestedCameraId = getIdForRequestedCamera(mFacing);
        if (requestedCameraId == -1) {
            throw new RuntimeException("Could not find requested camera.");
        }
        Camera camera = Camera.open(requestedCameraId);

        SizePair sizePair = selectSizePair(camera, requestedPreviewWidth, requestedPreviewHeight);
        if (sizePair == null) {
            throw new RuntimeException("Could not find suitable preview size.");
        }
        Size pictureSize = sizePair.pictureSize();
        previewSize = sizePair.previewSize();

        int[] previewFpsRange = selectPreviewFpsRange(camera, requestedFps);
        if (previewFpsRange == null) {
            throw new RuntimeException("Could not find suitable preview frames per second range.");
        }

        Camera.Parameters parameters = camera.getParameters();

        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        }

        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        parameters.setPreviewFpsRange(
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        parameters.setPreviewFormat(ImageFormat.NV21);

        setRotation(camera, parameters, requestedCameraId);

        if (focusMode != null) {
            if (parameters.getSupportedFocusModes().contains(
                    focusMode)) {
                parameters.setFocusMode(focusMode);
            } else {
                Log.i(TAG, "Camera focus mode: " + focusMode +
                        " is not supported on this device.");
            }
        }

        // setting focusMode to the one set in the params
        focusMode = parameters.getFocusMode();

        if (flashMode != null) {
            if (parameters.getSupportedFlashModes().contains(
                    flashMode)) {
                parameters.setFlashMode(flashMode);
            } else {
                Log.i(TAG, "Camera flash mode: " + flashMode +
                        " is not supported on this device.");
            }
        }

        // setting flashMode to the one set in the params
        flashMode = parameters.getFlashMode();

        camera.setParameters(parameters);

        camera.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));
        camera.addCallbackBuffer(createPreviewBuffer(previewSize));

        return camera;
    }
    /**
     * Gets the id for the camera specified by the direction it is facing.  Returns -1 if no such
     * camera was found.
     *
     * @param facing the desired camera (front-facing or rear-facing)
     */
    private static int getIdForRequestedCamera(int facing) {
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return i;
            }
        }
        return -1;
    }


    private static SizePair selectSizePair(Camera camera, int desiredWidth, int desiredHeight) {
        List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);
        SizePair selectedPair = null;
        int minDiff = Integer.MAX_VALUE;
        for (SizePair sizePair : validPreviewSizes) {
            Size size = sizePair.previewSize();
            int diff = Math.abs(size.getWidth() - desiredWidth) +
                    Math.abs(size.getHeight() - desiredHeight);
            if (diff < minDiff) {
                selectedPair = sizePair;
                minDiff = diff;
            }
        }

        return selectedPair;
    }
    /**
     * Stores a preview size and a corresponding same-aspect-ratio picture size.
     */
    private static class SizePair {
        private Size mPreview;
        private Size mPicture;

        public SizePair(android.hardware.Camera.Size previewSize,
                        android.hardware.Camera.Size pictureSize) {
            mPreview = new Size(previewSize.width, previewSize.height);
            if (pictureSize != null) {
                mPicture = new Size(pictureSize.width, pictureSize.height);
            }
        }

        public Size previewSize() {
            return mPreview;
        }

        @SuppressWarnings("unused")
        public Size pictureSize() {
            return mPicture;
        }
    }
    /**
     * Generates a list of acceptable preview sizes.  Preview sizes are not acceptable if there is
     * not a corresponding picture size of the same aspect ratio.
     */
    private static List<SizePair> generateValidPreviewSizeList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<android.hardware.Camera.Size> supportedPreviewSizes =
                parameters.getSupportedPreviewSizes();
        List<android.hardware.Camera.Size> supportedPictureSizes =
                parameters.getSupportedPictureSizes();
        List<SizePair> validPreviewSizes = new ArrayList<>();
        for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
            float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

            for (android.hardware.Camera.Size pictureSize : supportedPictureSizes) {
                float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(new SizePair(previewSize, pictureSize));
                    break;
                }
            }
        }

        if (validPreviewSizes.size() == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
            for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
                // The null picture size will let us know that we shouldn't set a picture size.
                validPreviewSizes.add(new SizePair(previewSize, null));
            }
        }

        return validPreviewSizes;
    }
    /**
     * Selects the most suitable preview frames per second range, given the desired frames per
     * second.
     *
     * @param camera the camera to select a frames per second range from
     * @param desiredPreviewFps the desired frames per second for the camera preview frames
     * @return the selected preview frames per second range
     */
    private int[] selectPreviewFpsRange(Camera camera, float desiredPreviewFps) {

        int desiredPreviewFpsScaled = (int) (desiredPreviewFps * 1000.0f);

        int[] selectedFpsRange = null;
        int minDiff = Integer.MAX_VALUE;
        List<int[]> previewFpsRangeList = camera.getParameters().getSupportedPreviewFpsRange();
        for (int[] range : previewFpsRangeList) {
            int deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
            int deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            int diff = Math.abs(deltaMin) + Math.abs(deltaMax);
            if (diff < minDiff) {
                selectedFpsRange = range;
                minDiff = diff;
            }
        }
        return selectedFpsRange;
    }

    /**
     * Calculates the correct rotation for the given camera id and sets the rotation in the
     * parameters.  It also sets the camera's display orientation and rotation.
     *
     * @param parameters the camera parameters for which to set the rotation
     * @param cameraId  the camera id to set rotation based on
     */
    private void setRotation(Camera camera, Camera.Parameters parameters, int cameraId) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int degrees = 0;
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                Log.e(TAG, "Bad rotation value: " + rotation);
        }

        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int angle;
        int displayAngle;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360;
            displayAngle = (360 - angle); // compensate for it being mirrored
        } else {  // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360;
            displayAngle = angle;
        }

        // This corresponds to the rotation constants in {@link Frame}.
        this.rotation = angle / 90;

        camera.setDisplayOrientation(displayAngle);
        parameters.setRotation(angle);
    }

    /**
     * Creates one buffer for the camera preview callback.  The size of the buffer is based off of
     * the camera preview size and the format of the camera image.
     *
     * @return a new preview buffer of the appropriate size for the current camera settings
     */
    private byte[] createPreviewBuffer(Size previewSize) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        long sizeInBits = previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
        int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

        byte[] byteArray = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        if (!buffer.hasArray() || (buffer.array() != byteArray)) {
            // I don't think that this will ever happen.  But if it does, then we wouldn't be
            // passing the preview content to the underlying detector later.
            throw new IllegalStateException("Failed to create valid buffer for camera source.");
        }

        bytesToByteBuffer.put(byteArray, buffer);
        return byteArray;
    }
    /**
     * Called when the camera has a new preview frame.
     */
    private class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            frameProcessor.setNextFrame(data, camera);
        }
    }

    /**
      This is designed to run detection on frames as fast as possible
     * While detection is running on a frame, new frames may be received from the camera.
     */
    private class FrameProcessingRunnable implements Runnable {
        private Detector<?> mDetector;
        private long mStartTimeMillis = SystemClock.elapsedRealtime();

        // This lock guards all of the member variables below.
        private final Object mLock = new Object();
        private boolean mActive = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private long mPendingTimeMillis;
        private int mPendingFrameId = 0;
        private ByteBuffer mPendingFrameData;

        FrameProcessingRunnable(Detector<?> detector) {
            mDetector = detector;
        }

        /**
         * Releases the underlying receiver.
         */
        @SuppressLint("Assert")
        void release() {
            assert (processingThread.getState() == State.TERMINATED);
            mDetector.release();
            mDetector = null;
        }
        /**
         * Marks the runnable as active/not active.  Signals any blocked threads to continue.
         */
        void setActive(boolean active) {
            synchronized (mLock) {
                mActive = active;
                mLock.notifyAll();
            }
        }

        /**
         * Sets the frame data received from the camera.
         */
        void setNextFrame(byte[] data, Camera camera) {
            synchronized (mLock) {
                if (mPendingFrameData != null) {
                    camera.addCallbackBuffer(mPendingFrameData.array());
                    mPendingFrameData = null;
                }

                if (!bytesToByteBuffer.containsKey(data)) {
                    Log.d(TAG,
                            "Skipping frame.  Could not find ByteBuffer associated with the image " +
                                    "data from the camera.");
                    return;
                }


                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis;
                mPendingFrameId++;
                mPendingFrameData = bytesToByteBuffer.get(data);

                // Notify the processor thread if it is waiting on the next frame (see below).
                mLock.notifyAll();
            }
        }
        /**
         * As long as the processing thread is active, this executes detection on frames continuously.
         */
        @Override
        public void run() {
            Frame outputFrame;
            ByteBuffer data;

            while (true) {
                synchronized (mLock) {
                    while (mActive && (mPendingFrameData == null)) {
                        try {

                            mLock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if (!mActive) {

                        return;
                    }

                    outputFrame = new Frame.Builder()
                            .setImageData(mPendingFrameData, previewSize.getWidth(),
                                    previewSize.getHeight(), ImageFormat.NV21)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation(rotation)
                            .build();

                    data = mPendingFrameData;
                    mPendingFrameData = null;
                }

                try {
                    mDetector.receiveFrame(outputFrame);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception thrown from receiver.", t);
                } finally {
                    camera.addCallbackBuffer(data.array());
                }
            }
        }
    }
}
