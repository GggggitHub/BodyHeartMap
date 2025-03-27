package com.example.bodyheartmap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * 人体热力图坐标系视图
 * 在热力图上绘制坐标系，显示像素坐标，不影响原图
 */
public class HeatMapCoordinateView extends View {
    private static final String TAG = "HeatMapCoordinateView";
    
    // 坐标系绘制相关参数
    private Paint axisPaint;       // 坐标轴画笔
    private Paint textPaint;       // 文字画笔
    private Paint gridPaint;       // 网格画笔
    
    // 坐标系参数
    private int axisColor = Color.WHITE;          // 坐标轴颜色
    private int textColor = Color.YELLOW;         // 文字颜色
    private int gridColor = Color.argb(80, 200, 200, 200); // 网格颜色（半透明）
    private float axisWidth = 2.0f;               // 坐标轴宽度
    private float textSize = 24.0f;               // 文字大小
    private int gridSpacing = 100;                // 网格间距（像素）
    private int labelOffset = 10;                 // 标签偏移量
    
    // 视图尺寸
    private int viewWidth;
    private int viewHeight;
    
    // 是否显示网格
    private boolean showGrid = true;

    public HeatMapCoordinateView(Context context) {
        super(context);
        init();
    }

    public HeatMapCoordinateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeatMapCoordinateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    /**
     * 初始化画笔和参数
     */
    private void init() {
        // 初始化坐标轴画笔
        axisPaint = new Paint();
        axisPaint.setColor(axisColor);
        axisPaint.setStrokeWidth(axisWidth);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setAntiAlias(true);
        
        // 初始化文字画笔
        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setAntiAlias(true);
        
        // 初始化网格画笔
        gridPaint = new Paint();
        gridPaint.setColor(gridColor);
        gridPaint.setStrokeWidth(1.0f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{5, 5}, 0));
        gridPaint.setAntiAlias(true);
        
        Log.d(TAG, "HeatMapCoordinateView初始化完成");
    }
    
    /**
     * 设置是否显示网格
     */
    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        invalidate();
    }
    
    /**
     * 设置网格间距（像素）
     */
    public void setGridSpacing(int spacing) {
        this.gridSpacing = spacing;
        invalidate();
    }
    
    /**
     * 设置文字大小
     */
    public void setCoordinateTextSize(float size) {
        this.textSize = size;
        textPaint.setTextSize(size);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        Log.d(TAG, "视图尺寸变化: 宽度=" + w + ", 高度=" + h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制网格
        if (showGrid) {
            drawGrid(canvas);
        }
        
        // 绘制坐标轴
        drawAxis(canvas);
        
        // 绘制刻度和标签
        drawScalesAndLabels(canvas);
    }
    
    /**
     * 绘制网格
     */
    private void drawGrid(Canvas canvas) {
        // 绘制垂直网格线
        for (int x = gridSpacing; x < viewWidth; x += gridSpacing) {
            canvas.drawLine(x, 0, x, viewHeight, gridPaint);
        }
        
        // 绘制水平网格线
        for (int y = gridSpacing; y < viewHeight; y += gridSpacing) {
            canvas.drawLine(0, y, viewWidth, y, gridPaint);
        }
    }
    
    /**
     * 绘制坐标轴
     */
    private void drawAxis(Canvas canvas) {
        // 绘制X轴
        canvas.drawLine(0, viewHeight - 1, viewWidth, viewHeight - 1, axisPaint);
        
        // 绘制Y轴
        canvas.drawLine(1, 0, 1, viewHeight, axisPaint);
        
        // 绘制坐标轴标签
        canvas.drawText("X", viewWidth - 30, viewHeight - 10, textPaint);
        canvas.drawText("Y", 10, 30, textPaint);
        
        // 绘制原点标签
        canvas.drawText("O(0,0)", 10, viewHeight - 10, textPaint);
    }
    
    /**
     * 绘制刻度和标签
     */
    private void drawScalesAndLabels(Canvas canvas) {
        // 绘制X轴刻度和标签
        for (int x = gridSpacing; x < viewWidth; x += gridSpacing) {
            // 绘制刻度线
            canvas.drawLine(x, viewHeight - 5, x, viewHeight + 5, axisPaint);
            
            // 绘制刻度值
            String xLabel = String.valueOf(x);
            float textWidth = textPaint.measureText(xLabel);
            canvas.drawText(xLabel, x - textWidth / 2, viewHeight - labelOffset - textSize, textPaint);
        }
        
        // 绘制Y轴刻度和标签
        for (int y = gridSpacing; y < viewHeight; y += gridSpacing) {
            // 计算实际Y坐标（从底部向上）
            int actualY = viewHeight - y;
            
            // 绘制刻度线
            canvas.drawLine(-5, actualY, 5, actualY, axisPaint);
            
            // 绘制刻度值
            String yLabel = String.valueOf(y);
            canvas.drawText(yLabel, labelOffset + 5, actualY + textSize / 3, textPaint);
        }
    }
    
    /**
     * 将像素坐标转换为视图坐标
     */
    public float[] pixelToViewCoordinates(float pixelX, float pixelY) {
        return new float[]{pixelX, viewHeight - pixelY};
    }
    
    /**
     * 将视图坐标转换为像素坐标
     */
    public float[] viewToPixelCoordinates(float viewX, float viewY) {
        return new float[]{viewX, viewHeight - viewY};
    }
}