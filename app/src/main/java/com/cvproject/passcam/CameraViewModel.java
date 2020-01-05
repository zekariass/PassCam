package com.cvproject.passcam;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.face.Face;
import org.opencv.face.Facemark;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CameraViewModel extends ViewModel {

    private CascadeClassifier mCascadeClassifier;
    private Mat outputMat[] = null;

    private File mModelFile;
    private Facemark fmarker;
    private MatOfPoint3f modelPoints;
    private MatOfPoint2f imgPoints;
    private double focaLength;
    private Point center;
    private Mat cameraMatrix;
    private MatOfDouble distCoeffs;
    private Mat rMatrix;
    private Mat tMatrix;
    public MutableLiveData<Double> yaw;
    public MutableLiveData<Double> pitch;
    public MutableLiveData<Double> roll;
    public MutableLiveData<String> capStateColor;
    public MutableLiveData<String> toastTextForCaptureStatus;
    private Context mContext;
    public CameraViewModel(){
        yaw = new MutableLiveData<>(0.0);
        pitch = new MutableLiveData<>(0.0);
        roll = new MutableLiveData<>(0.0);
        capStateColor = new MutableLiveData<>("#fd0204");
        toastTextForCaptureStatus = new MutableLiveData<>("No face found!!!");
    }

    public void startInitializers(Context context, Mat frameScale){
        mContext = context;
        rMatrix = new Mat();
        tMatrix = new Mat();
        initializeFileForCascade(context);
        initializeFaceMarkObject(context);
        initializeWorldPoints();
        initializeCameraInternals(frameScale);

    }

    private void initializeFaceMarkObject(Context context) {

        InputStream is = context.getResources().openRawResource(R.raw.lbfmodel);
        File modelDir = context.getDir("face_model", Context.MODE_PRIVATE);
        mModelFile = new File(modelDir, "lbfmodel.yaml");
        try {
            FileOutputStream os = new FileOutputStream(mModelFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        }catch (Exception e){
            Log.i("FILE ERROR", "Something wrong when reading the cascade file");
        }
        //======INSTANTIATE FACEMARK OBJECT AND LOAD THE MODEL=======
        fmarker = Face.createFacemarkLBF();
        fmarker.loadModel(mModelFile.getAbsolutePath());
        //==========================================================
    }

    private void initializeFileForCascade(Context context) {
        InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_default);
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
        try {
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        }catch (Exception e){
            Log.i("FILE ERROR", "Something wrong when reading the cascade file");
        }

        mCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
    }

    private void initializeWorldPoints() {

        List<Point3> points3d = new ArrayList<>();
        modelPoints = new MatOfPoint3f();

         points3d.add(new Point3(0.0f, -330.0f, -65.0f)); //Chin
        points3d.add(new Point3(0.0f,0.0f,0.0f)); // Nose tip
        points3d.add(new Point3(225.0f, 170.0f, -135.0f)); //Right eye right corner
        points3d.add(new Point3(-225.0f, 170.0f, -135.0f)); //Left eye left corner
        points3d.add(new Point3(-150.0f, -150.0f, -125.0f)); //Left mouth corner
        points3d.add(new Point3(150.0f, -150.0f, -125.0f)); //Right mouth corner

        modelPoints.fromList(points3d);

    }


    private void initializeCameraInternals(Mat frameScale) {
        focaLength = frameScale.cols();
        center = new Point((frameScale.cols()/2.0), (frameScale.rows()/2.0));
        cameraMatrix = new Mat(3,3, CvType.CV_64F);

        double[] matrixData = {focaLength, 0D, center.x, 0D, focaLength, center.y, 0D, 0D, 1D};
        cameraMatrix.put(0,0,matrixData);
        distCoeffs = new MatOfDouble(Mat.zeros(4,1,CvType.CV_64F));

    }

    public Mat doCascading(Mat mInputFrame, int absoluteFaceSize, Mat matGrayScaleFrame, boolean drawPoseCoord){

        Imgproc.cvtColor(mInputFrame, matGrayScaleFrame, Imgproc.COLOR_BGR2GRAY);
        MatOfRect faces = new MatOfRect();
        if (mCascadeClassifier != null) {
            mCascadeClassifier.detectMultiScale(matGrayScaleFrame, faces, 1.1,
                    3, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }else {
            Log.i("FACE: ", "mCascadeClassifier is null");
        }
        Rect[] facesArray = faces.toArray();

        if (facesArray.length > 0){
            for (int i = 0; i <facesArray.length; i++)
            {
                Imgproc.rectangle(mInputFrame, facesArray[i].tl(), facesArray[i].br(),
                        new Scalar(255, 255, 0, 255), 2);
            }
            return findLandMarks(mInputFrame, faces, drawPoseCoord);

        }else{
            capStateColor.postValue("#fd0204"); //RED if there is no face
            toastTextForCaptureStatus.postValue("No face found!!!");
            return mInputFrame;
        }

    }


    private Mat findLandMarks(Mat mInputFrame, MatOfRect faces, boolean drawPoseCoord) {
        imgPoints = new MatOfPoint2f();
        ArrayList<MatOfPoint2f> landmarks = new ArrayList<>();
        fmarker.fit(mInputFrame, faces, landmarks);
        List<Point> point2d = new ArrayList<>();
        point2d.clear();
        for (int i=0; i<landmarks.size(); i++){
            MatOfPoint2f lms = landmarks.get(i);
            for (int j=0; j<lms.rows(); j++){

                double[] dp = lms.get(j,0);
                Point p = new Point(dp[0], dp[1]);
                if (j==8||j==60||j==54||j==30||j==36||j==45){
                    if (!drawPoseCoord){
                        Imgproc.circle(mInputFrame, p, 2, new Scalar(222), 3);
                    }
                    point2d.add(new Point(p.x, p.y));
                }
            }
        }
        imgPoints.fromList(point2d);
        return estimateHeadPose(mInputFrame, drawPoseCoord);
    }

    private Mat estimateHeadPose(Mat mInputFrame, boolean drawPoseCoord) {

        Mat rotMatrix;
        rotMatrix = HeadPoseEstimator.estimatePose(mInputFrame,modelPoints, imgPoints, cameraMatrix,
                distCoeffs, rMatrix, tMatrix, mContext, drawPoseCoord);
        getEuAngles(rotMatrix);
        return mInputFrame;

    }

    private void getEuAngles(Mat rotMat){

        Mat euAng = EulerAngleFindHelper.findEurlerAngles(rotMat);

        int sizeOfEuAng = (int) euAng.total();
        double[] angles = new double[sizeOfEuAng];
        euAng.get(0,0,angles);

        yaw.postValue(angles[1]);
        pitch.postValue(angles[0]);
        roll.postValue(angles[2]);

        if ((-10.0< pitch.getValue() && pitch.getValue() < 10.0) &&
                (-10.0 < yaw.getValue() && yaw.getValue() < 10.0) &&
                -10.0 < roll.getValue() && roll.getValue() < 10.0){

            capStateColor.postValue("#00ff00");
            toastTextForCaptureStatus.postValue("Picture has been captured!!!");
        }else {
            capStateColor.postValue("#fd0204");
            toastTextForCaptureStatus.postValue("Incorrect head pose!!!");
        }
    }


    @Override
    protected void onCleared() {
        super.onCleared();

    }
}
