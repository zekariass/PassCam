package com.cvproject.passcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class HeadPoseEstimator {
    public static Mat estimatePose(Mat mInputFrame, MatOfPoint3f modelPoints, MatOfPoint2f imgPoints, Mat cameraMatrix,
                                   MatOfDouble distCoeffs, Mat rMatrix, Mat tMatrix, Context mContext, boolean drawPoseCoord){
        try {
            Calib3d.solvePnP(modelPoints, imgPoints, cameraMatrix, distCoeffs, rMatrix, tMatrix,
                    false, Calib3d.SOLVEPNP_ITERATIVE);
        }catch (Exception e){
            Log.e("SOLVEPNP ERROR: ", e.getMessage());
        }

        double[] point1 = modelPoints.get(0,0);
        double arrowLength = 1000.0;
        MatOfPoint3f projectPoints = new MatOfPoint3f(
                new Point3(point1[0],point1[1],point1[2]),
                new Point3(arrowLength + point1[0],0.0+point1[1],0.0+point1[2]),
                new Point3(0.0 + point1[0],arrowLength+point1[1],0.0+point1[2]),
                new Point3(0.0 + point1[0],0.0+point1[1],arrowLength+point1[2])
        );
        MatOfPoint2f noseEndPoint2D = new MatOfPoint2f();
        Calib3d.projectPoints(projectPoints, rMatrix, tMatrix, cameraMatrix, distCoeffs, noseEndPoint2D);
        String dump = noseEndPoint2D.dump();
        List<Point> pts = noseEndPoint2D.toList();
        if (!drawPoseCoord){
            Imgproc.arrowedLine(mInputFrame, pts.get(0), pts.get(1), new Scalar(0,0,255), 5);
            Imgproc.arrowedLine(mInputFrame, pts.get(0), pts.get(2), new Scalar(255,255,0), 5);
            Imgproc.arrowedLine(mInputFrame, pts.get(0), pts.get(3), new Scalar(255,0,0), 5);
        }
        double[] rot = {0D,0D,0D,0D,0D,0D,0D,0D,0D};
        Mat rotMatrix = new Mat(3,3, CvType.CV_64FC1);
        rotMatrix.put(0,0,rot);
        Calib3d.Rodrigues(rMatrix,rotMatrix);
        return rotMatrix;
    }
}
