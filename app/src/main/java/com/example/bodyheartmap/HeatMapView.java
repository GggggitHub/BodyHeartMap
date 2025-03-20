package com.example.bodyheartmap;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class HeatMapView extends GLSurfaceView {
    private HeatMapRenderer renderer;

    public HeatMapView(Context context) {
        super(context);
        init(context);
    }

    public HeatMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // 设置OpenGL ES 2.0上下文
        setEGLContextClientVersion(2);
        
        // 创建渲染器
        renderer = new HeatMapRenderer(context);
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
}