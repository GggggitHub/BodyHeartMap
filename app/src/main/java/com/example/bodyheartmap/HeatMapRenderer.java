package com.example.bodyheartmap;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class HeatMapRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    
    // 人体模型
    private BodyModel bodyModel;
    
    // 温度数据
    private float[] temperatureData;
    
    // 着色器程序
    private int program;
    private int positionHandle;
    private int texCoordHandle;
    private int temperatureHandle;
    private int colorMapHandle;
    private int mvpMatrixHandle;
    
    // 颜色映射纹理
    private int colorMapTexture;
    
    // 变换矩阵
    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    // 在类成员变量中添加
    private boolean needsRedraw;
    
    public HeatMapRenderer(Context context) {
        this.context = context;
        bodyModel = new BodyModel();
        setupTemperatureData();
    }
    
    private void setupTemperatureData() {
        // 初始化温度数据，对应每个身体部位的顶点
        temperatureData = new float[] {
            36.5f, 36.6f, 36.7f, 36.5f, // 头部
            36.7f, 36.9f, 37.1f, 36.8f, // 躯干
            36.6f, 36.4f, 36.5f, 36.7f, // 左臂
            36.7f, 36.5f, 36.6f, 36.8f, // 右臂
            36.3f, 36.4f, 36.6f, 36.5f, // 左腿
            36.4f, 36.5f, 36.7f, 36.6f  // 右腿
        };
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        
        float ratio = (float) width / height;
        
        // 使用正交投影而不是透视投影，更适合2D显示
        Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, -10, 10);
        
        // 设置视图矩阵
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        
        // 计算变换矩阵
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
    
    // 修改片段着色器代码，支持透明度
    private String getFragmentShaderCode() {
        return 
            "precision mediump float;\n" +
            "varying vec2 texCoord;\n" +
            "uniform sampler2D uColorMap;\n" +
            "void main() {\n" +
            "  // 使用纹理坐标的x分量作为温度值，y分量作为透明度\n" +
            "  float normalizedTemp = texCoord.x;\n" + 
            "  float alpha = texCoord.y;\n" + // 从纹理坐标获取透明度
            "  vec4 color = texture2D(uColorMap, vec2(normalizedTemp, 0.5));\n" +
            "  gl_FragColor = vec4(color.rgb, color.a * alpha);\n" + // 应用透明度
            "}\n"; // 确保这里有结束的花括号
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

    // 修改片段着色器代码，支持透明度
    private String getFragmentShaderCodeOld() {
        return "precision mediump float;\n" +
                        "varying vec2 texCoord;\n" +
                        "uniform sampler2D uColorMap;\n" +
                        "void main() {\n" +
                        "  // 使用纹理坐标的x值作为热力图索引\n" +
                        "  float heatIndex = texCoord.x;\n" +
                        "  gl_FragColor = texture2D(uColorMap, vec2(heatIndex, 0.5));\n" +
                        "}\n";
    }

    // 修改顶点着色器代码，确保正确传递纹理坐标
    private String getVertexShaderCodeOld() {
        return
                "uniform mat4 uMVPMatrix;\n" +
                        "attribute vec4 vPosition;\n" +
                        "attribute vec2 vTexCoord;\n" +
                        "varying vec2 texCoord;\n" +
                        "void main() {\n" +
                        "  gl_Position = uMVPMatrix * vPosition;\n" +
                        "  texCoord = vTexCoord;\n" +
                        "}\n";
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

        // 设置背景色为透明
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

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

        // 启用混合，使颜色能够正确显示
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        System.out.println("着色器程序创建完成，ID: " + program);
        System.out.println("位置句柄: " + positionHandle);
        System.out.println("纹理坐标句柄: " + texCoordHandle);
        System.out.println("颜色映射句柄: " + colorMapHandle);
        System.out.println("矩阵句柄: " + mvpMatrixHandle);
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
        
        // 设置顶点属性
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, bodyModel.getVertexBuffer());
        
        // 设置纹理坐标 - 确保每次都重新绑定最新的纹理坐标
        if (texCoordHandle != -1) {
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, bodyModel.getTexCoordBuffer());
        } else {
            System.err.println("纹理坐标句柄无效");
        }
        
        // 设置变换矩阵
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        // 设置颜色映射纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorMapTexture);
        GLES20.glUniform1i(colorMapHandle, 0);
        
        // 绘制人体各部位
        drawBodyPart(bodyModel.getHeadIndices());
        drawBodyPart(bodyModel.getTorsoIndices());
        drawBodyPart(bodyModel.getLeftArmIndices());
        drawBodyPart(bodyModel.getRightArmIndices());
        drawBodyPart(bodyModel.getLeftLegIndices());
        drawBodyPart(bodyModel.getRightLegIndices());
        
        // 禁用顶点数组
        GLES20.glDisableVertexAttribArray(positionHandle);
        if (texCoordHandle != -1) {
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }
        
        // 重置需要重绘标志
        needsRedraw = false;
        
        System.out.println("人体热力图渲染完成");
    }
    
    // 添加drawBodyPart方法，确保使用正确的绘制模式
    private void drawBodyPart(int[] indices) {
        if (indices != null && indices.length >= 2) {
            // 使用TRIANGLE_FAN模式绘制身体部位
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, indices[0], indices[1]);
        }
    }
}