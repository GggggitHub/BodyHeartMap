package com.example.bodyheartmap;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class HeatMapRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "HeatMapRenderer";
    private final Context context;
    
    // 人体模型
    private BodyModel bodyModel;
    
    // 温度数据
    private float[] temperatureData;
    
    // 着色器程序
    private int program;
    private int positionHandle;
    private int texCoordHandle;
    private int colorMapHandle;
    private int mvpMatrixHandle;
    
    // 颜色映射纹理
    private int colorMapTexture;
    
    // 变换矩阵
    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    
    public HeatMapRenderer(Context context) {
        this.context = context;
        bodyModel = new BodyModel(context);
        setupTemperatureData();
    }
    
    private void setupTemperatureData() {
        // 初始化温度数据，对应13个身体部位
        temperatureData = new float[] {
            36.5f, // 头部
            36.6f, // 颈部
            36.7f, // 胸部
            36.8f, // 腹部
            36.6f, // 左肩
            36.4f, // 左臂
            36.3f, // 左手
            36.7f, // 右肩
            36.5f, // 右臂
            36.4f, // 右手
            36.3f, // 左大腿
            36.2f, // 左小腿
            36.4f, // 右大腿
            36.3f  // 右小腿
        };
    }

    // 在类成员变量中添加
    private float scaleFactor = 0.3f; // 固定缩放因子为0.3
    private boolean scaleChanged = true; // 标记缩放是否改变
    private int[] viewport = new int[4]; // 用于存储视口信息

    // 修改setScaleFactor方法，确保缩放因子的变化被检测到
    public void setScaleFactor(float newScaleFactor) {
        // 添加最小值限制，防止除以零或极小值
        if (newScaleFactor < 0.1f) {
            newScaleFactor = 0.1f;
        }

        // 只有当缩放因子真正改变时才更新
        if (Math.abs(this.scaleFactor - newScaleFactor) > 0.001f) {
            this.scaleFactor = newScaleFactor;
            scaleChanged = true; // 标记缩放已改变
            Log.d("HeatMapRenderer", "缩放因子已设置为: " + newScaleFactor);
        }
    }
    
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        updateProjectionMatrix(width, height);
    }


    // 在类成员变量中添加
    private float offsetX = 0.0f; // X轴偏移量
    private float offsetY = 0.0f; // Y轴偏移量
    private boolean positionChanged = false; // 位置是否改变

    // 设置X轴偏移
    public void setOffsetX(float offsetX) {
        if (this.offsetX != offsetX) {
            this.offsetX = offsetX;
            positionChanged = true;
            Log.d("HeatMapRenderer", "X轴偏移已设置为: " + offsetX);
        }
    }

    // 设置Y轴偏移
    public void setOffsetY(float offsetY) {
        if (this.offsetY != offsetY) {
            this.offsetY = offsetY;
            positionChanged = true;
            Log.d("HeatMapRenderer", "Y轴偏移已设置为: " + offsetY);
        }
    }

    // 在类的成员变量部分添加
    public static boolean useNormalizedCoordinates = true;
    public static boolean useOld = true;

    // 修改投影矩阵计算方法
    private void updateProjectionMatrix(int width, int height) {
        Log.d(TAG, "updateProjectionMatrix() called with: width = [" + width + "], height = [" + height + "]");

        float ratio = (float) width / height;

        // 重置投影矩阵
        Matrix.setIdentityM(projectionMatrix, 0);


        // 使用正交投影，直接使用scaleFactor缩放视口
        // 注意：较大的scaleFactor会使图像变小，较小的scaleFactor会使图像变大
        // 使用正交投影，应用缩放因子和偏移量
        float left = -ratio  ;
        float right = ratio  ;
        float bottom = -1.0f  ;
        float top = 1.0f  ;

        left = -ratio / scaleFactor + offsetX;
        right = ratio / scaleFactor + offsetX;
        bottom = -1.0f / scaleFactor + offsetY;
        top = 1.0f / scaleFactor + offsetY;



        // 获取人体模型边界信息
        float[] boundaries = bodyModel.getBoundaries();
        float minX = boundaries[0];
        float maxX = boundaries[1];
        float minY = boundaries[2];
        float maxY = boundaries[3];

        // 模型中心点（务必确保此处计算正确）
        float centerX = (minX + maxX) / 2f; // 必须为 504
        float centerY = (minY + maxY) / 2f; // 必须为 1253.5

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

        Matrix.orthoM(projectionMatrix, 0, left, right, bottom, top, 0.1f, 100.0f);

        // 记录当前使用的视口范围，用于调试
        Log.d("HeatMapRenderer", "视口范围: left=" + left + ", right=" + right +
                ", bottom=" + bottom + ", top=" + top +
                ", scaleFactor=" + scaleFactor +
                ", offsetX=" + offsetX + ", offsetY=" + offsetY);

        // 设置视图矩阵，将相机放在z轴上
        Matrix.setLookAtM(viewMatrix, 0,
                0, 0, 3.0f,  // 相机位置
                0, 0, 0,     // 观察点
                0, 1.0f, 0); // 上向量

        // 计算最终的MVP矩阵
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    private void createColorMapTexture() {
        // 创建热力图颜色映射纹理
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        colorMapTexture = textures[0];
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorMapTexture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        
        // 创建颜色映射数据 - 从蓝色到红色的渐变
        ByteBuffer colorMap = ByteBuffer.allocateDirect(256 * 4);
        colorMap.order(ByteOrder.nativeOrder());
        
        for (int i = 0; i < 256; i++) {
            float t = i / 255.0f;
            int r, g, b;
            
            if (t < 0.25f) {
                // 蓝色到青色
                r = 0;
                g = (int)(255 * (t / 0.25f));
                b = 255;
            } else if (t < 0.5f) {
                // 青色到绿色
                r = 0;
                g = 255;
                b = (int)(255 * (1 - (t - 0.25f) / 0.25f));
            } else if (t < 0.75f) {
                // 绿色到黄色
                r = (int)(255 * ((t - 0.5f) / 0.25f));
                g = 255;
                b = 0;
            } else {
                // 黄色到红色
                r = 255;
                g = (int)(255 * (1 - (t - 0.75f) / 0.25f));
                b = 0;
            }
            
            colorMap.put((byte) r);
            colorMap.put((byte) g);
            colorMap.put((byte) b);
            colorMap.put((byte) 255); // Alpha 设为完全不透明
        }
        colorMap.position(0);
        
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 256, 1, 0, 
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, colorMap);
        
        System.out.println("颜色映射纹理创建完成，ID: " + colorMapTexture);
    }


    private float alpha = 0.7f; // 默认透明度

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    /**
     # 全局透明度和局部透明度的工作原理
     是的，您的代码中确实有两个透明度值在配合工作：

     1. 全局透明度(uAlpha) ：这是通过uniform变量传递给着色器的，代表整个热力图的透明度。这个值是通过 setAlpha() 方法设置的，通常由UI上的透明度滑动条控制。
     2. 局部透明度(texCoord.y) ：这是通过纹理坐标的y分量传递给着色器的，代表每个顶点的局部透明度。这个值是在 BodyModel 类中设置的，当调用 updateTextureCoordinates() 方法时会更新。

     它们的配合工作方式如下：
     float alpha = texCoord.y * uAlpha;

     这行代码将两个透明度值相乘，得到最终应用于像素的透明度值。这意味着：

     - 如果全局透明度(uAlpha)为1.0，则最终透明度完全由局部透明度(texCoord.y)决定
     - 如果全局透明度(uAlpha)为0.0，则无论局部透明度是多少，最终都是完全透明的
     - 如果局部透明度(texCoord.y)为0.0，则该像素完全透明，无论全局透明度是多少
     - 其他情况下，最终透明度是两者的乘积，这样可以实现更细致的透明度控制
     这种设计的优点是：

     1. 您可以通过滑动条控制整个热力图的整体透明度
     2. 同时，您可以为不同的身体部位设置不同的基础透明度
     3. 两者结合，可以实现非常灵活的透明度效果

     * @return
     */
    private String getFragmentShaderCode() {
        return 
                "precision mediump float;\n" +
                "varying vec2 texCoord;\n" +
                "uniform sampler2D uColorMap;\n" +
                "uniform float uAlpha;\n" + // 全局透明度
                "void main() {\n" +
                "  float normalizedTemp = texCoord.x;\n" + 
                "  float alpha = texCoord.y * uAlpha;\n" + // 结合局部和全局透明度
                "  vec4 color = texture2D(uColorMap, vec2(normalizedTemp, 0.5));\n" +
                "  gl_FragColor = vec4(color.rgb, alpha);\n" + // 设置透明度
                "}\n";
    }


    // 修改顶点着色器代码，确保所有花括号匹配
    private String getVertexShaderCode() {
        return 
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec4 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * vPosition;\n" +
            "  texCoord = vTexCoord;\n" +
            "}\n"; // 确保这里有结束的花括号
    }


    // Add loadShader method
    private int loadShader(int type, String shaderCode) {
        // 创建着色器对象
        int shader = GLES20.glCreateShader(type);
        
        // 打印着色器代码以便调试
        System.out.println("着色器代码 (" + (type == GLES20.GL_VERTEX_SHADER ? "顶点" : "片段") + "):\n" + shaderCode);
        
        // 添加源代码并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        
        // 检查编译状态
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String errorMsg = GLES20.glGetShaderInfoLog(shader);
            System.err.println("Shader compilation error: " + errorMsg);
            System.err.println("着色器编译失败，无法继续");
            GLES20.glDeleteShader(shader);
            return 0;
        }
        
        System.out.println((type == GLES20.GL_VERTEX_SHADER ? "顶点" : "片段") + "着色器编译成功");
        return shader;
    }
    
    // 更新温度数据，并支持透明度
    public void updateTemperature(float[] temperatures, float alpha) {
        this.alpha = alpha;
        if (temperatures != null && temperatures.length >= 6) {
            // 复制温度数据
            System.arraycopy(temperatures, 0, temperatureData, 0, Math.min(temperatures.length, temperatureData.length));
            
            // 更新BodyModel的纹理坐标
            if (bodyModel != null) {
                bodyModel.updateTextureCoordinates(temperatures, alpha);
                
                // 打印日志确认温度和透明度更新
                System.out.println("温度数据和透明度已更新: " + 
                                  "头部=" + temperatures[0] + ", " + 
                                  "躯干=" + temperatures[1] + ", " + 
                                  "左臂=" + temperatures[2] + ", " + 
                                  "右臂=" + temperatures[3] + ", " + 
                                  "左腿=" + temperatures[4] + ", " + 
                                  "右腿=" + temperatures[5] + ", " +
                                  "透明度=" + alpha);
            } else {
                System.err.println("bodyModel为空，无法更新纹理坐标");
            }
        } else {
            System.err.println("无效的温度数据长度，需要至少6个值对应6个身体部位");
        }
    }
    
    // 保持原有的方法，但调用新方法并使用默认透明度
    public void updateTemperature(float[] temperatures) {
        // 默认透明度为1.0（完全不透明）
        updateTemperature(temperatures, 1.0f);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置背景色为完全透明
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    
        // 启用深度测试，确保正确的绘制顺序
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        
        //启用混合
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    
        // 创建颜色映射纹理
        createColorMapTexture();
    
        // 编译着色器
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShaderCode());
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShaderCode());
    
        // 检查着色器编译是否成功
        if (vertexShader == 0 || fragmentShader == 0) {
            System.err.println("着色器编译失败，无法继续");
            return;
        }
    
        // 创建着色器程序
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    
        // 检查链接状态
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            String log = GLES20.glGetProgramInfoLog(program);
            System.err.println("程序链接失败: " + log);
            GLES20.glDeleteProgram(program);
            program = 0;
            return;
        }
    
        // 获取着色器变量句柄
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord");
        colorMapHandle = GLES20.glGetUniformLocation(program, "uColorMap");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
    
        // 输出更详细的调试信息
        Log.d("HeatMapRenderer", "着色器程序创建完成，ID: " + program);
        Log.d("HeatMapRenderer", "位置句柄: " + positionHandle);
        Log.d("HeatMapRenderer", "纹理坐标句柄: " + texCoordHandle);
        Log.d("HeatMapRenderer", "颜色映射句柄: " + colorMapHandle);
        Log.d("HeatMapRenderer", "矩阵句柄: " + mvpMatrixHandle);
        
        // 检查BodyModel是否正确初始化
        if (bodyModel != null) {
            FloatBuffer vertexBuffer = bodyModel.getVertexBuffer();
            FloatBuffer texCoordBuffer = bodyModel.getTexCoordBuffer();
            if (vertexBuffer != null && texCoordBuffer != null) {
                Log.d("HeatMapRenderer", "顶点缓冲区容量: " + vertexBuffer.capacity());
                Log.d("HeatMapRenderer", "纹理坐标缓冲区容量: " + texCoordBuffer.capacity());
            } else {
                Log.e("HeatMapRenderer", "顶点缓冲区或纹理坐标缓冲区为空");
            }
        } else {
            Log.e("HeatMapRenderer", "BodyModel为空");
        }
    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // 检查程序是否有效
        if (program == 0) {
            System.err.println("程序无效，无法渲染");
            return;
        }
        
        // 使用着色器程序
        GLES20.glUseProgram(program);
    
        // 设置透明度uniform
        int alphaHandle = GLES20.glGetUniformLocation(program, "uAlpha");
        GLES20.glUniform1f(alphaHandle, alpha);

        // 如果缩放因子改变，重新计算投影矩阵
        if (scaleChanged || positionChanged) {
            // 获取当前视口尺寸
            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0);
            updateProjectionMatrix(viewport[2], viewport[3]);
            scaleChanged = false;
            positionChanged = false;
            // 打印日志确认缩放因子已应用
            Log.d("HeatMapRenderer", "应用新的缩放因子: " + scaleFactor );
        }

        // 确保设置MVP矩阵 - 这行是关键，确保矩阵被传递给着色器
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
    
        // 设置顶点属性
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, bodyModel.getVertexBuffer());
        GLES20.glEnableVertexAttribArray(positionHandle);
    
        // 设置纹理坐标属性
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, bodyModel.getTexCoordBuffer());
        GLES20.glEnableVertexAttribArray(texCoordHandle);
    
        // 设置颜色映射纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorMapTexture);
        GLES20.glUniform1i(colorMapHandle, 0);
    
        // 绘制所有身体部位
        Map<String, int[]> bodyPartIndices = bodyModel.getBodyPartIndices();
        for (Map.Entry<String, int[]> entry : bodyPartIndices.entrySet()) {
            int[] indices = entry.getValue();
            int startIndex = indices[0];
            int vertexCount = indices[1];
            
            // 使用三角形扇形绘制每个身体部位
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, startIndex, vertexCount);
        }
    
        // 禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

}