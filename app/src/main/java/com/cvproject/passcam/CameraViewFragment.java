package com.cvproject.passcam;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.cvproject.passcam.databinding.FragmentCameraViewBinding;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraViewFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private PassCameraView mOpenCameraViewBase;
    private Mat grayscaleImg;
    private Mat colorImg;
    private Mat beforeFlp;
    private FragmentCameraViewBinding binding;
    private int absoluteFaceSizeInScreen;
    private Mat outFrame;
    private  CameraViewModel viewModel;
    private static final String getImgLocation = Environment.getExternalStorageDirectory()+ File.separator + "PassCam";
    private Mat dest, forward;
    private String backgroudColor;
    private SharedPreferences camFlags;
    private SharedPreferences.Editor camFlagsEditor;
    private boolean isFront;
    private boolean drawCoord;
    private ImageView imgCaptureBtn;
    private ImageView imgFlipBtn;
    private ImageView imgCoordBtn;
    private boolean drawPoseCoord;

    public CameraViewFragment() { }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCameraViewBase.enableView();
                    break;
                }default:
                {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentCameraViewBinding.inflate(inflater);
        mOpenCameraViewBase = binding.cameraViewFragment;
        mOpenCameraViewBase.setVisibility(SurfaceView.VISIBLE);
        mOpenCameraViewBase.setCvCameraViewListener(this);
        camFlags = getActivity().getApplicationContext().getSharedPreferences("CAMFLAGS", Context.MODE_PRIVATE);

        camFlagsEditor = camFlags.edit();
        if (camFlags == null){
            camFlagsEditor.putBoolean("DRAWCOORD", false);
            camFlagsEditor.putBoolean("FLIPCAM", false);
            camFlagsEditor.apply();
        }

        imgCaptureBtn = binding.captureButton;
        imgFlipBtn = binding.flipCameraButton;
        imgCoordBtn = binding.drawCoordButton;

        drawPoseCoord = camFlags.getBoolean("DRAWCOORD", false);
        if (!drawPoseCoord){
            imgCoordBtn.setBackgroundResource(R.drawable.clear_coord_24px);
        }else {
            imgCoordBtn.setBackgroundResource(R.drawable.create_coord_24px);
        }

        viewModel = ViewModelProviders.of(this).get(CameraViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);
        backgroudColor = "#fd0204";


        MutableLiveData<String> toastCapStatusText = viewModel.toastTextForCaptureStatus;

        imgCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String outPic = getImgLocation + File.separator + Utilities.generateFileName();
                Log.i("FILE PATH: ",outPic);
                Utilities.createDefaultDir(getImgLocation);
                if (backgroudColor.equals("#fd0204")){
                    Toast.makeText(getContext(), viewModel.toastTextForCaptureStatus.getValue(), Toast.LENGTH_LONG).show();
                }else {
                    mOpenCameraViewBase.takePicture(outPic);
                    Toast.makeText(CameraViewFragment.this.getContext(), "Picture has been taken ", Toast.LENGTH_LONG).show();
                }
                Log.d("PIC", "Path " + outPic);
            }
        });


        imgFlipBtn.setOnClickListener((View v)->{
            isFront = camFlags.getBoolean("FLIPCAM", false);
            if (!isFront){
                camFlagsEditor.putBoolean("FLIPCAM", !isFront);
                mOpenCameraViewBase.disableView();
                mOpenCameraViewBase.setCameraIndex(1);
                mOpenCameraViewBase.enableView();
            }else {
                camFlagsEditor.putBoolean("FLIPCAM", !isFront);
                mOpenCameraViewBase.disableView();
                mOpenCameraViewBase.setCameraIndex(0);
                mOpenCameraViewBase.enableView();
            }
            camFlagsEditor.apply();

        });


        imgCoordBtn.setOnClickListener((View v)->{
            drawCoordDrawIcon();
        });


        viewModel.yaw.observe(this, yaw -> {
            binding.yawTextview.setText("Yaw: "+ Utilities.roundDoubleAndStringtize(yaw));
        });

        viewModel.pitch.observe(this, pitch ->{
            binding.pitchTextview.setText("Pitch: "+ Utilities.roundDoubleAndStringtize(pitch));
        });

        viewModel.roll.observe(this, roll ->{
            binding.rollTextview.setText("Roll: "+ Utilities.roundDoubleAndStringtize(roll));
        });

        viewModel.capStateColor.observe(this, bgColor -> {
            backgroudColor = bgColor;
            binding.mainContainerView.setBackgroundColor(Color.parseColor(bgColor));
        });

        return binding.getRoot();
    }

    private void drawCoordDrawIcon(){
        camFlagsEditor.putBoolean("DRAWCOORD", !drawPoseCoord);
        drawPoseCoord = !drawPoseCoord;
        camFlagsEditor.apply();
        if(drawPoseCoord){
            imgCoordBtn.setBackgroundResource(R.drawable.create_coord_24px);
        }else{
            imgCoordBtn.setBackgroundResource(R.drawable.clear_coord_24px);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCameraViewBase != null){
            mOpenCameraViewBase.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this.getContext(), mLoaderCallback);
        }else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mOpenCameraViewBase != null){
            mOpenCameraViewBase.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        grayscaleImg = new Mat(height, width, CvType.CV_8UC4);
        colorImg = new Mat(height, width, CvType.CV_8UC4);
       // beforeFlp = new Mat(height, width, CvType.CV_8UC4);
        dest = new Mat(height, width, CvType.CV_8UC4);
        forward = new Mat(height, width, CvType.CV_8UC4);

        absoluteFaceSizeInScreen = (int) (height * 0.2);// The faces will be a 20% of the height of the screen
        viewModel.startInitializers(this.getContext(), grayscaleImg);
    }

    @Override
    public void onCameraViewStopped() {

        grayscaleImg.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        colorImg = inputFrame.rgba();

        boolean camDirection = camFlags.getBoolean("FLIPCAM", false);
        if (camDirection){
            Core.flip(colorImg, colorImg, 1);
        }
        outFrame = viewModel.doCascading(colorImg,absoluteFaceSizeInScreen, grayscaleImg, drawPoseCoord);
        return outFrame;
    }

}
