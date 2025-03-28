package com.aj.bodyheartmap.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class HeatMapView extends GLSurfaceView {
    private HeatMapRenderer renderer;
    private float scaleFactor = 1f; // 设置固定缩放因子为0.3

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
        // 添加最小值限制
        if (scaleFactor < 0.1f) {
            scaleFactor = 0.1f;
        }
        
        this.scaleFactor = scaleFactor;
        if (renderer != null) {
            renderer.setScaleFactor(scaleFactor);
            Log.d("HeatMapView", "设置缩放因子: " + scaleFactor);
            // 强制立即重绘
            requestRender();
        } else {
            Log.e("HeatMapView", "渲染器为空，无法设置缩放因子");
        }
    }

    // 添加一个方法用于测试不同的缩放值
    public void testScaling() {
        // 测试不同的缩放值
        new Thread(() -> {
            try {
                for (float scale = 0.1f; scale <= 2.0f; scale += 0.1f) {
                    final float testScale = scale;
                    post(() -> {
                        setScaleFactor(testScale);
                        Log.d("HeatMapView", "测试缩放: " + testScale);
                    });
                    Thread.sleep(1000); // 每秒更改一次缩放
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private float offsetX = 0.0f; // X轴偏移量
    private float offsetY = 0.0f; // Y轴偏移量

    // 设置X轴偏移
    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
        if (renderer != null) {
            renderer.setOffsetX(offsetX);
            Log.d("HeatMapView", "设置X轴偏移: " + offsetX);
            requestRender(); // 强制重新渲染
        }
    }

    // 设置Y轴偏移
    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
        if (renderer != null) {
            renderer.setOffsetY(offsetY);
            Log.d("HeatMapView", "设置Y轴偏移: " + offsetY);
            requestRender(); // 强制重新渲染
        }
    }

    // 向上移动
    public void moveUp(float step) {
        setOffsetY(offsetY - step);
    }

    // 向下移动
    public void moveDown(float step) {
        setOffsetY(offsetY + step);
    }

    // 向左移动
    public void moveLeft(float step) {
        setOffsetX(offsetX + step);
    }

    // 向右移动
    public void moveRight(float step) {
        setOffsetX(offsetX - step);
    }
}