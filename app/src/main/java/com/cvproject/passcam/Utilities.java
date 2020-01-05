package com.cvproject.passcam;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utilities {
    private Utilities(){}
    public static String roundDoubleAndStringtize(double value){

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.toString();
    }

    public static String generateFileName(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return "img" + sdf.format(new Date()) + ".jpg";
    }

    public static void createDefaultDir(String dirPath){
        File dir = new File(dirPath);
        if (!dir.exists()){
            dir.mkdir();
        }
    }
}
