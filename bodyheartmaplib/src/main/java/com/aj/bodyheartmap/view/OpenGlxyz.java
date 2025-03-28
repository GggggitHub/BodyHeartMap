package com.aj.bodyheartmap.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by 123 on 2023/12/12.
 *
 * 3D坐标系渲染器
 */
public class OpenGlxyz extends GLSurfaceView {
    private static final String TAG = "OpenGlxyz";
    private CoordinateRenderer renderer;

    public OpenGlxyz(Context context) {
        super(context);
        init(context);
        Log.i(TAG, "OpenGlxyz: ");
    }

    public OpenGlxyz(Context context, AttributeSet attrs) {
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
        renderer = new CoordinateRenderer();
        setRenderer(renderer);
        
        // 设置渲染模式为连续渲染
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        // 设置保留EGL上下文
        setPreserveEGLContextOnPause(true);
        
        Log.d(TAG, "OpenGlxyz初始化完成");
    }

    // 添加到OpenGlxyz类中 - 正确的方法
    public void setAxisScale(float scale) {
        if (renderer != null) {
            renderer.setAxisScale(scale);
            requestRender();
        }
    }
    
    // 旋转视角
    public void rotateView(float angleX, float angleY) {
        // 可以添加旋转视角的功能，让用户更好地观察坐标系
        // 这里需要修改renderer中的viewMatrix
    }

    // 坐标系渲染器
    private static class CoordinateRenderer implements GLSurfaceView.Renderer {
        private static final String VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;" +
                "attribute vec4 aPosition;" +
                "attribute vec4 aColor;" +
                "uniform float uPointSize;" +  // 添加点大小uniform变量
                "varying vec4 vColor;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * aPosition;" +
                "  gl_PointSize = uPointSize;" +  // 设置点大小
                "  vColor = aColor;" +
                "}";

        private static final String FRAGMENT_SHADER =
                "precision mediump float;" +
                "varying vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}";

        // 坐标轴数据：X轴(红色)、Y轴(绿色)、Z轴(蓝色)
        private static final float[] AXIS_COORDS = {
                // X轴 (红色)
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                
                // Y轴 (绿色)
                0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                
                // Z轴 (蓝色)
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                
                // 斜线 (黄色) - 从原点到(1,1,1)
                0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f
        };
    
        // X轴刻度线 (每200像素一个刻度)
        private static final float[] X_TICKS = {
                // 200像素刻度
                0.2f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.2f, -0.05f, 0.0f, 1.0f, 0.0f, 0.0f,
                
                // 400像素刻度
                0.4f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.4f, -0.05f, 0.0f, 1.0f, 0.0f, 0.0f,
                
                // 600像素刻度
                0.6f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.6f, -0.05f, 0.0f, 1.0f, 0.0f, 0.0f,
                
                // 800像素刻度
                0.8f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.8f, -0.05f, 0.0f, 1.0f, 0.0f, 0.0f,
                
                // 1000像素刻度
                1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, -0.05f, 0.0f, 1.0f, 0.0f, 0.0f
        };
    
        // Y轴刻度线 (每200像素一个刻度)
        private static final float[] Y_TICKS = {
                // 200像素刻度
                0.0f, 0.2f, 0.0f, 0.0f, 1.0f, 0.0f,
                -0.05f, 0.2f, 0.0f, 0.0f, 1.0f, 0.0f,
                
                // 400像素刻度
                0.0f, 0.4f, 0.0f, 0.0f, 1.0f, 0.0f,
                -0.05f, 0.4f, 0.0f, 0.0f, 1.0f, 0.0f,
                
                // 600像素刻度
                0.0f, 0.6f, 0.0f, 0.0f, 1.0f, 0.0f,
                -0.05f, 0.6f, 0.0f, 0.0f, 1.0f, 0.0f,
                
                // 800像素刻度
                0.0f, 0.8f, 0.0f, 0.0f, 1.0f, 0.0f,
                -0.05f, 0.8f, 0.0f, 0.0f, 1.0f, 0.0f,
                
                // 1000像素刻度
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                -0.05f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
        };
    
        // Z轴刻度线 (每200像素一个刻度)
        private static final float[] Z_TICKS = {
                // 200像素刻度
                0.0f, 0.0f, 0.2f, 0.0f, 0.0f, 1.0f,
                -0.05f, 0.0f, 0.2f, 0.0f, 0.0f, 1.0f,
                
                // 400像素刻度
                0.0f, 0.0f, 0.4f, 0.0f, 0.0f, 1.0f,
                -0.05f, 0.0f, 0.4f, 0.0f, 0.0f, 1.0f,
                
                // 600像素刻度
                0.0f, 0.0f, 0.6f, 0.0f, 0.0f, 1.0f,
                -0.05f, 0.0f, 0.6f, 0.0f, 0.0f, 1.0f,
                
                // 800像素刻度
                0.0f, 0.0f, 0.8f, 0.0f, 0.0f, 1.0f,
                -0.05f, 0.0f, 0.8f, 0.0f, 0.0f, 1.0f,
                
                // 1000像素刻度
                0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                -0.05f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f
        };
    
        // 坐标轴标签 (X, Y, Z)
        private static final float[] AXIS_LABELS = {
                // X标签位置和颜色 (红色)
                1.1f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                
                // Y标签位置和颜色 (绿色)
                0.0f, 1.1f, 0.0f, 0.0f, 1.0f, 0.0f,
                
                // Z标签位置和颜色 (蓝色)
                0.0f, 0.0f, 1.1f, 0.0f, 0.0f, 1.0f
        };
    
        // 刻度值标签 (200, 400, 600, 800, 1000)
        private static final float[] TICK_LABELS = {
                // X轴刻度值
                0.2f, -0.1f, 0.0f, 1.0f, 1.0f, 1.0f,  // 200
                0.4f, -0.1f, 0.0f, 1.0f, 1.0f, 1.0f,  // 400
                0.6f, -0.1f, 0.0f, 1.0f, 1.0f, 1.0f,  // 600
                0.8f, -0.1f, 0.0f, 1.0f, 1.0f, 1.0f,  // 800
                1.0f, -0.1f, 0.0f, 1.0f, 1.0f, 1.0f,  // 1000
                
                // Y轴刻度值
                -0.1f, 0.2f, 0.0f, 1.0f, 1.0f, 1.0f,  // 200
                -0.1f, 0.4f, 0.0f, 1.0f, 1.0f, 1.0f,  // 400
                -0.1f, 0.6f, 0.0f, 1.0f, 1.0f, 1.0f,  // 600
                -0.1f, 0.8f, 0.0f, 1.0f, 1.0f, 1.0f,  // 800
                -0.1f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f   // 1000
        };

        private final FloatBuffer vertexBuffer;
        private final FloatBuffer xTicksBuffer;
        private final FloatBuffer yTicksBuffer;
        private final FloatBuffer zTicksBuffer;
        private final FloatBuffer axisLabelsBuffer;
        private final FloatBuffer tickLabelsBuffer;
        
        private int program;
        private int positionHandle;
        private int colorHandle;
        private int mvpMatrixHandle;
        private int pointSizeHandle;  // 添加点大小句柄

        // MVP矩阵
        private final float[] mvpMatrix = new float[16];
        private final float[] projectionMatrix = new float[16];
        private final float[] viewMatrix = new float[16];
        private final float[] modelMatrix = new float[16];
        
        // 添加缩放因子
        private float axisScale = 1.0f;

        public CoordinateRenderer() {
            // 初始化顶点缓冲区
            ByteBuffer bb = ByteBuffer.allocateDirect(AXIS_COORDS.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(AXIS_COORDS);
            vertexBuffer.position(0);
            
            // 初始化X轴刻度缓冲区
            ByteBuffer xTicksBB = ByteBuffer.allocateDirect(X_TICKS.length * 4);
            xTicksBB.order(ByteOrder.nativeOrder());
            xTicksBuffer = xTicksBB.asFloatBuffer();
            xTicksBuffer.put(X_TICKS);
            xTicksBuffer.position(0);
            
            // 初始化Y轴刻度缓冲区
            ByteBuffer yTicksBB = ByteBuffer.allocateDirect(Y_TICKS.length * 4);
            yTicksBB.order(ByteOrder.nativeOrder());
            yTicksBuffer = yTicksBB.asFloatBuffer();
            yTicksBuffer.put(Y_TICKS);
            yTicksBuffer.position(0);
            
            // 初始化Z轴刻度缓冲区
            ByteBuffer zTicksBB = ByteBuffer.allocateDirect(Z_TICKS.length * 4);
            zTicksBB.order(ByteOrder.nativeOrder());
            zTicksBuffer = zTicksBB.asFloatBuffer();
            zTicksBuffer.put(Z_TICKS);
            zTicksBuffer.position(0);
            
            // 初始化坐标轴标签缓冲区
            ByteBuffer axisLabelsBB = ByteBuffer.allocateDirect(AXIS_LABELS.length * 4);
            axisLabelsBB.order(ByteOrder.nativeOrder());
            axisLabelsBuffer = axisLabelsBB.asFloatBuffer();
            axisLabelsBuffer.put(AXIS_LABELS);
            axisLabelsBuffer.position(0);
            
            // 初始化刻度值标签缓冲区
            ByteBuffer tickLabelsBB = ByteBuffer.allocateDirect(TICK_LABELS.length * 4);
            tickLabelsBB.order(ByteOrder.nativeOrder());
            tickLabelsBuffer = tickLabelsBB.asFloatBuffer();
            tickLabelsBuffer.put(TICK_LABELS);
            tickLabelsBuffer.position(0);
        }
        
        // 设置坐标轴缩放 - 正确的位置
        public void setAxisScale(float scale) {
            this.axisScale = scale;
            // 重新计算MVP矩阵
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.scaleM(modelMatrix, 0, axisScale, axisScale, axisScale);
            
            // 更新MVP矩阵
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
            
            Log.d(TAG, "坐标轴缩放设置为: " + axisScale);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.d(TAG, "onSurfaceCreated: 开始创建OpenGL表面");
            
            // 设置清屏颜色为透明
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            
            // 编译着色器
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            
            // 创建OpenGL程序
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            
            // 获取着色器变量句柄
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
            colorHandle = GLES20.glGetAttribLocation(program, "aColor");
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
            pointSizeHandle = GLES20.glGetUniformLocation(program, "uPointSize");  // 获取点大小句柄
            
            // 启用深度测试
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d(TAG, "onSurfaceChanged: 宽度=" + width + ", 高度=" + height);
            
            GLES20.glViewport(0, 0, width, height);
            
            float ratio = (float) width / height;
            
            // Matrix.frustumM (透视投影)设置透视投影
            //### 参数解释：
            //- projectionMatrix : 存储结果矩阵的数组
            //- 0 : 结果矩阵在数组中的起始偏移量
            //- -ratio, ratio : 近平面的左右边界（根据屏幕宽高比调整）
            //- -1, 1 : 近平面的底部和顶部边界
            //- 1, 10 : 近平面和远平面的距离
            //### 特点：
            //- 产生透视效果，远处的物体看起来更小
            //- 平行线会在远处相交（消失点效果）
            //- 模拟人眼或相机的自然视觉效果
            //- 适合绘制真实感的3D场景、游戏、虚拟现实等
//            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
            
            // 设置相机位置
            //这行代码定义了相机（或观察者）在 3D 空间中的位置和朝向：
            //
            //- viewMatrix : 存储视图矩阵的数组
            //- 3, 3, 3 : 相机的位置坐标，位于 (3,3,3) 点，即在 X、Y、Z 轴的正方向上
            //- 0, 0, 0 : 相机看向的点，这里是原点 (0,0,0)
            //- 0, 1, 0 : 相机的"上"方向，这里是 Y 轴正方向
            //这个设置使相机位于第一象限，从一个45度角俯视原点，这样可以同时看到 X、Y、Z 三个坐标轴。
//            Matrix.setLookAtM(viewMatrix, 0,
//                    3, 3, 3,  // 相机位置 (右上角45度角观察)
//                    0, 0, 0,  // 观察点 (原点)
//                    0, 1, 0); // 相机上方向量

            //TODO 下面是 热力图的视角

            //Matrix.orthoM (正交投影)
            //- projectionMatrix : 存储结果矩阵的数组
            //- 0 : 结果矩阵在数组中的起始偏移量
            //- -ratio, ratio : 视景体的左右边界（根据屏幕宽高比调整）
            //- -1, 1 : 视景体的底部和顶部边界
            //- 0.1f, 100.0f : 视景体的近平面和远平面距离
            //这个设置创建了一个正交投影矩阵，将 3D 场景映射到 2D 屏幕上。
            //### 特点：
            //- 不会产生透视效果，远处的物体不会变小
            //- 平行线在投影后仍然保持平行
            //- 物体的大小不会随着距离变化而变化
            //- 适合绘制2D界面、工程图纸、等距视图等
            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 0.1f, 100.0f);

            // 设置视图矩阵，将相机放在z轴上
            Matrix.setLookAtM(viewMatrix, 0,
                    0, 0, 3.0f,  // 相机位置
                    0, 0, 0,     // 观察点
                    0, 1.0f, 0); // 上向量

            // 设置模型矩阵为单位矩阵
            Matrix.setIdentityM(modelMatrix, 0);
            
            // 应用缩放
            if (axisScale != 1.0f) {
                Matrix.scaleM(modelMatrix, 0, axisScale, axisScale, axisScale);
            }
            
            // 计算最终的MVP矩阵
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // 添加周期性日志，避免日志过多
            if (Math.random() < 0.01) { // 只有1%的帧会输出日志
                Log.d(TAG, "onDrawFrame: 绘制坐标系");
            }
            
            // 清除颜色缓冲区和深度缓冲区
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            
            // 使用OpenGL程序
            GLES20.glUseProgram(program);
            
            // 设置MVP矩阵
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
            
            // 启用顶点属性
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glEnableVertexAttribArray(colorHandle);
            
            // 绘制坐标轴
            vertexBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, vertexBuffer);
            
            vertexBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, vertexBuffer);
            
            GLES20.glLineWidth(5.0f); // 设置线宽
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8); // 4对线段，共8个顶点
            
            // 绘制X轴刻度
            xTicksBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, xTicksBuffer);
            
            xTicksBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, xTicksBuffer);
            
            GLES20.glLineWidth(2.0f); // 设置刻度线宽
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 10); // 5对刻度线，共10个顶点
            
            // 绘制Y轴刻度
            yTicksBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, yTicksBuffer);
            
            yTicksBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, yTicksBuffer);
            
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 10); // 5对刻度线，共10个顶点
            
            // 绘制Z轴刻度
            zTicksBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, zTicksBuffer);
            
            zTicksBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, zTicksBuffer);
            
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 10); // 5对刻度线，共10个顶点
            
            // 绘制坐标轴标签点 (用于标识XYZ位置)
            axisLabelsBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, axisLabelsBuffer);
            
            axisLabelsBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, axisLabelsBuffer);
            
            // 设置点大小为10.0
            GLES20.glUniform1f(pointSizeHandle, 10.0f);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 3); // 3个标签点
            
            // 绘制刻度值标签点
            tickLabelsBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, tickLabelsBuffer);
            
            tickLabelsBuffer.position(3);
            GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 
                    6 * 4, tickLabelsBuffer);
            
            // 设置点大小为5.0
            GLES20.glUniform1f(pointSizeHandle, 5.0f);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 10); // 10个刻度值标签点
            
            // 禁用顶点属性
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(colorHandle);
        }

        // 加载着色器
        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            
            // 检查编译状态
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                Log.e(TAG, "着色器编译错误: " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
            
            return shader;
        }
    }
}