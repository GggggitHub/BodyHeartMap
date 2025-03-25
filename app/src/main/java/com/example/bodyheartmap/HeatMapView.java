package com.example.bodyheartmap;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class HeatMapView extends GLSurfaceView {
    private HeatMapRenderer renderer;
    private float scaleFactor = 0.3f; // 设置固定缩放因子为0.3

    public HeatMapView(Context context) {
        super(context);
        init(context);
    }

    // 解决热力图透明度问题
    // 您提到热力图的透明度有变化，但仍然看不到后面的图片和背景。这很可能是因为OpenGL渲染的透明度设置不正确。我们需要修改几个地方来解决这个问题。
    // 修改HeatMapView
    // 首先，我们需要确保HeatMapView支持透明背景：
    public HeatMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        // 设置为透明背景
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        
        init(context);
    }

    private void init(Context context) {
        // 设置OpenGL ES 2.0上下文
        setEGLContextClientVersion(2);
        
        // 创建渲染器
        renderer = new HeatMapRenderer(context);
        // 设置缩放因子
        renderer.setScaleFactor(scaleFactor);
        setRenderer(renderer);
        
        // 设置渲染模式为连续渲染，用于调试
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        // 设置保留EGL上下文
        setPreserveEGLContextOnPause(true);
    }
    
    // 更新温度数据
    public void updateTemperatureData(float[] temperatures) {
        renderer.updateTemperature(temperatures);
        requestRender(); // 请求重新渲染
    }
    // 更新温度数据
    public void updateTemperatureData(float[] temperatures, float alpha) {
        renderer.updateTemperature(temperatures,alpha);
        requestRender(); // 请求重新渲染
    }

    public void updateGlAlpha(float currentAlpha) {
        renderer.setAlpha(currentAlpha);
        requestRender(); // 请求重新渲染
    }
    
    // 设置缩放因子
    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        if (renderer != null) {
            renderer.setScaleFactor(scaleFactor);
            Log.d("HeatMapView", "设置缩放因子: " + scaleFactor);
            requestRender(); // 强制重新渲染
        } else {
            Log.e("HeatMapView", "渲染器为空，无法设置缩放因子");
        }
    }
}