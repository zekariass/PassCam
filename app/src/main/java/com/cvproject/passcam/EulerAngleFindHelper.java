package com.cvproject.passcam;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class EulerAngleFindHelper {
    public static Mat findEurlerAngles(Mat eulerMat){
        Mat camMat, rotMat, tranMat, rotMatX, rotMatY, rotMatZ, euAngMat;
        camMat = new Mat();
        rotMat = new Mat();
        tranMat = new Mat();
        rotMatX = new Mat();
        rotMatY = new Mat();
        rotMatZ = new Mat();
        euAngMat = new Mat();
        int size = (int) eulerMat.total();
        double[] rv = new double[size];
        eulerMat.get(0,0,rv);
        Mat projectMatric = new Mat(3,4, CvType.CV_64FC1);
        double[] projMat = {rv[0],rv[1],rv[2],0,
                rv[3],rv[4],rv[5],0,
                rv[6],rv[7],rv[8],0};
        projectMatric.put(0,0,projMat);
        Calib3d.decomposeProjectionMatrix(projectMatric, camMat, rotMat, tranMat, rotMatX, rotMatY, rotMatZ, euAngMat);
        return euAngMat;
    }
}
