package org.opencv.samples.tutorial1;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.*;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Tutorial1Activity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    // Stores State - First or Second Photo Taken?
    private enum PhotoActivityState {
        PUZZLE_PIECES, PUZZLE_BOX
    }
    private PhotoActivityState state;
    private long origSaveTime = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Tutorial1Activity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        // Update State Based on Intent Settings
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Log.i(TAG, extras.getString("photoActivityState"));
        } else {
            Log.i(TAG, "no extras");
        }

        if (extras != null && extras.getString("photoActivityState").equals("PUZZLE_BOX")) {
            state = PhotoActivityState.PUZZLE_BOX;
        } else {
            state = PhotoActivityState.PUZZLE_PIECES;
        }

        Log.i(TAG, state.toString());

        // Get Original Save Time, If Exists
        if (extras != null) {
            origSaveTime = extras.getLong("origSaveTime");
        }

        // Update Label Based on State
        TextView tv = (TextView) findViewById(R.id.imageCaptureLabel);
        if (state == PhotoActivityState.PUZZLE_BOX) {
            tv.setText("Take Picture of Box");
        } else if (state == PhotoActivityState.PUZZLE_PIECES) {
            tv.setText("Take Picture of Pieces");
        }


    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    boolean touched = false;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        touched = true;
        Toast toast = Toast.makeText(getApplicationContext(), "Picture Captured!", Toast.LENGTH_SHORT);
        toast.show();
        return true;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();

        if (touched) {

            Log.i("Tutorial1Activity", "Touched");

            // Convert Image to Grayscale
            Mat mGray = new Mat();
            Imgproc.cvtColor(rgba, mGray, Imgproc.COLOR_RGB2GRAY);

            FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
            DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

            String filename = "opencv.yml";
            File outputFile = new File(this.getCacheDir(), filename);
            try {
                FileOutputStream stream = new FileOutputStream(outputFile);
                stream.write("%YAML:1.0\nscaleFactor: 1.1\nnLevels: 10\nfirstLevel: 0\nedgeThreshold: 31\npatchSize: 31\n".getBytes());
                stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            extractor.read(outputFile.getAbsolutePath());

            Mat descriptors = new Mat();
            MatOfKeyPoint keypoints = new MatOfKeyPoint();

            detector.detect(rgba, keypoints);

            Mat mBgr = new Mat();
            Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
            saveImage(mBgr);

            /*Mat featureImage = new Mat();
            Scalar kpColor = new Scalar(255,159,10);
            Features2d.drawKeypoints(mBgr, keypoints, featureImage, kpColor, 0);

            // Convert Image to Bgr
            String imageFilePath = saveImage(featureImage);

            Mat origBgr = new Mat();
            Imgproc.cvtColor(rgba, origBgr, Imgproc.COLOR_RGBA2BGR);

            Mat detectedEdges = new Mat();
            Mat edgesImage = new Mat(rgba.size(), rgba.type());
            Mat edgesBurger = new Mat();
            Imgproc.blur(mGray, detectedEdges, new Size(3,3));
            Imgproc.Canny(detectedEdges, detectedEdges, 50.0, 150.0, 3);

            origBgr.copyTo(edgesImage, detectedEdges);

            String imageFilePath2 = saveImage(edgesImage);*/

            // Open Image View
            Intent intent;
//            intent.putExtra("imageToDisplay", imageFilePath);
//            intent.putExtra("imageToDisplay2", imageFilePath2);

            if (state == PhotoActivityState.PUZZLE_PIECES) {
                intent = new Intent(getBaseContext(), Tutorial1Activity.class);
                intent.putExtra("photoActivityState", "PUZZLE_BOX");
            } else {
                intent = new Intent(getBaseContext(), PointDisplayActivity.class);
            }
            intent.putExtra("origSaveTime", origSaveTime);

            startActivity(intent);

            touched = false;

        }

        return rgba;
    }

    public String saveImage(Mat mBgr) {

        long saveTime = System.currentTimeMillis();

        // If We Have the Save Time from the Last Activity, Use It
        if (origSaveTime != 0) {
            saveTime = origSaveTime;
        }

        File sd = Environment.getExternalStorageDirectory();

        File pieceDirectory = new File(sd, "jig/pieces/");
        File boxDirectory = new File(sd, "jig/box/");
        pieceDirectory.mkdirs();
        boxDirectory.mkdirs();

        // Save in Separate Folder Based on Mode
        String filename = saveTime + ".png";
        File file;
        if (state == PhotoActivityState.PUZZLE_PIECES) {
            file = new File(pieceDirectory, filename);
        } else {
            file = new File(boxDirectory, filename);
        }

        filename = file.toString();
        Log.i(TAG, filename);
        Boolean imageSaved = Imgcodecs.imwrite(filename, mBgr);

        if (imageSaved) {
            Log.i(TAG, "SUCCESS writing image to external storage");
        } else {
            Log.i(TAG, "Fail writing image to external storage");
        }

        // Set Save Time Based on How we Saved the File
        origSaveTime = saveTime;

        return file.getAbsolutePath();

    }

}
