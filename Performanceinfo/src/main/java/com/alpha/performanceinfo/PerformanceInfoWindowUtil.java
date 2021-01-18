package com.alpha.performanceinfo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.alpha.perfermanceinfo.PerformanceManager;

import static com.alpha.perfermanceinfo.PerformanceManagerKt.RED_WARNING_CPU;
import static com.alpha.perfermanceinfo.PerformanceManagerKt.RED_WARNING_FPS;
import static com.alpha.perfermanceinfo.PerformanceManagerKt.RED_WARNING_MEM;
import static com.alpha.perfermanceinfo.PerformanceManagerKt.WARNING_CPU;
import static com.alpha.perfermanceinfo.PerformanceManagerKt.WARNING_FPS;
import static com.alpha.perfermanceinfo.PerformanceManagerKt.WARNING_MEM;

public class PerformanceInfoWindowUtil {

    private ViewGroup contactView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private Context mContext;
    private TextView tvCpu;
    private TextView tvMem;
    private TextView tvFps;

    public PerformanceInfoWindowUtil(Context context) {
        mContext = context;
        windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
//      windowManager = (WindowManager) App.getAppContext().getSystemService(Context.WINDOW_SERVICE);
    }

    public void showContactView() {
        hideContactView();
        contactView = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.view_info, null);
        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.x = 0;
            layoutParams.y = 0;
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 23) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
            }
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            layoutParams.format = PixelFormat.RGBA_8888;
        }

        tvCpu = contactView.findViewById(R.id.tv_cpu);
        tvMem = contactView.findViewById(R.id.tv_mem);
        tvFps = contactView.findViewById(R.id.tv_fps);


        contactView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        windowManager.addView(contactView, layoutParams);
    }



    public void hideAllView() {
        hideContactView();
    }

    public void hideContactView() {
        if (contactView != null) {
            windowManager.removeView(contactView);
            contactView = null;
        }
    }


    public void refreshInfo(float cpu, float mem, float fps){

        if (cpu > RED_WARNING_CPU){
            tvCpu.setTextColor(Color.RED);
        }else if (cpu > WARNING_CPU){
            tvCpu.setTextColor(Color.YELLOW);
        }else {
            tvCpu.setTextColor(Color.GREEN);
        }

        if (mem > RED_WARNING_MEM){
            tvMem.setTextColor(Color.RED);
        }else if (mem > WARNING_MEM){
            tvMem.setTextColor(Color.YELLOW);
        }else {
            tvMem.setTextColor(Color.GREEN);
        }

        if (fps < RED_WARNING_FPS){
            tvFps.setTextColor(Color.RED);
        }if (fps < WARNING_FPS){
            tvFps.setTextColor(Color.YELLOW);
        }else {
            tvFps.setTextColor(Color.GREEN);
        }

        tvCpu.setText("CPU: "+cpu +" %");
        tvMem.setText("MEM: "+mem + " MB");
        tvFps.setText("FPS: "+fps+"");
    }


}
