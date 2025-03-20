package com.example.bodyheartmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BodyModel {
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    
    // 身体各部位的顶点索引范围
    private int[] headIndices;
    private int[] torsoIndices;
    private int[] leftArmIndices;
    private int[] rightArmIndices;
    private int[] leftLegIndices;
    private int[] rightLegIndices;
    
    public BodyModel() {
        setupVertexData();
    }
    
    private void setupVertexData() {
        // 人体模型顶点坐标 - 更精确的人体轮廓
        float[] vertices = {
            // 头部 (6个顶点形成圆形)
            0.0f,  0.85f, 0.0f,  // 顶部中心
            -0.1f, 0.75f, 0.0f,  // 左上
            -0.15f, 0.65f, 0.0f, // 左侧
            0.0f,  0.6f, 0.0f,   // 底部
            0.15f, 0.65f, 0.0f,  // 右侧
            0.1f,  0.75f, 0.0f,  // 右上
            
            // 躯干 (6个顶点)
            -0.2f, 0.6f, 0.0f,   // 左肩
            -0.25f, 0.3f, 0.0f,  // 左腰
            -0.2f, 0.0f, 0.0f,   // 左胯
            0.2f,  0.0f, 0.0f,   // 右胯
            0.25f, 0.3f, 0.0f,   // 右腰
            0.2f,  0.6f, 0.0f,   // 右肩
            
            // 左臂 (6个顶点)
            -0.2f, 0.6f, 0.0f,   // 肩部连接点
            -0.3f, 0.55f, 0.0f,  // 上臂外侧
            -0.4f, 0.4f, 0.0f,   // 肘部
            -0.45f, 0.25f, 0.0f, // 前臂外侧
            -0.4f, 0.15f, 0.0f,  // 手腕
            -0.3f, 0.3f, 0.0f,   // 内侧回连
            
            // 右臂 (6个顶点)
            0.2f,  0.6f, 0.0f,   // 肩部连接点
            0.3f,  0.55f, 0.0f,  // 上臂外侧
            0.4f,  0.4f, 0.0f,   // 肘部
            0.45f, 0.25f, 0.0f,  // 前臂外侧
            0.4f,  0.15f, 0.0f,  // 手腕
            0.3f,  0.3f, 0.0f,   // 内侧回连
            
            // 左腿 (6个顶点)
            -0.2f, 0.0f, 0.0f,   // 胯部连接点
            -0.25f, -0.2f, 0.0f, // 大腿外侧
            -0.2f, -0.4f, 0.0f,  // 膝盖
            -0.25f, -0.6f, 0.0f, // 小腿外侧
            -0.2f, -0.8f, 0.0f,  // 脚踝
            -0.1f, 0.0f, 0.0f,   // 内侧回连
            
            // 右腿 (6个顶点)
            0.2f,  0.0f, 0.0f,   // 胯部连接点
            0.25f, -0.2f, 0.0f,  // 大腿外侧
            0.2f,  -0.4f, 0.0f,  // 膝盖
            0.25f, -0.6f, 0.0f,  // 小腿外侧
            0.2f,  -0.8f, 0.0f,  // 脚踝
            0.1f,  0.0f, 0.0f    // 内侧回连
        };
        
        // 纹理坐标 - 根据温度设置不同的值
        // 使用x坐标作为热力图索引 (0.0-1.0)
        float[] texCoords = {
            // 头部 - 中等温度 (0.5-0.6)
            0.55f, 0.5f,
            0.53f, 0.5f,
            0.52f, 0.5f,
            0.54f, 0.5f,
            0.56f, 0.5f,
            0.58f, 0.5f,
            
            // 躯干 - 较高温度 (0.6-0.8)
            0.65f, 0.5f,
            0.70f, 0.5f,
            0.75f, 0.5f,
            0.75f, 0.5f,
            0.70f, 0.5f,
            0.65f, 0.5f,
            
            // 左臂 - 中等温度 (0.4-0.5)
            0.45f, 0.5f,
            0.43f, 0.5f,
            0.42f, 0.5f,
            0.40f, 0.5f,
            0.41f, 0.5f,
            0.44f, 0.5f,
            
            // 右臂 - 中等温度 (0.5-0.6)
            0.52f, 0.5f,
            0.53f, 0.5f,
            0.55f, 0.5f,
            0.56f, 0.5f,
            0.54f, 0.5f,
            0.53f, 0.5f,
            
            // 左腿 - 较低温度 (0.3-0.4)
            0.35f, 0.5f,
            0.33f, 0.5f,
            0.32f, 0.5f,
            0.30f, 0.5f,
            0.31f, 0.5f,
            0.34f, 0.5f,
            
            // 右腿 - 较低温度 (0.3-0.5)
            0.40f, 0.5f,
            0.38f, 0.5f,
            0.36f, 0.5f,
            0.35f, 0.5f,
            0.37f, 0.5f,
            0.39f, 0.5f
        };
        
        // 创建顶点缓冲区
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        
        // 创建纹理坐标缓冲区
        ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
        
        // 设置身体部位索引 - 第一个值是起始索引，第二个值是顶点数量
        headIndices = new int[]{0, 6};
        torsoIndices = new int[]{6, 6};
        leftArmIndices = new int[]{12, 6};
        rightArmIndices = new int[]{18, 6};
        leftLegIndices = new int[]{24, 6};
        rightLegIndices = new int[]{30, 6};
    }
    
    public FloatBuffer getVertexBuffer() {
        return vertexBuffer;
    }
    
    public FloatBuffer getTexCoordBuffer() {
        return texCoordBuffer;
    }
    
    public int[] getHeadIndices() {
        return headIndices;
    }
    
    public int[] getTorsoIndices() {
        return torsoIndices;
    }
    
    public int[] getLeftArmIndices() {
        return leftArmIndices;
    }
    
    public int[] getRightArmIndices() {
        return rightArmIndices;
    }
    
    public int[] getLeftLegIndices() {
        return leftLegIndices;
    }
    
    public int[] getRightLegIndices() {
        return rightLegIndices;
    }
    
    // 添加一个方法来更新纹理坐标
    public void updateTextureCoordinates(float[] temperatures) {
        if (temperatures == null || temperatures.length < 6) {
            System.err.println("温度数据不足，无法更新纹理坐标");
            return;
        }
        
        // 创建新的纹理坐标数组
        float[] texCoords = new float[36 * 2]; // 6个部位，每个部位6个顶点，每个顶点2个纹理坐标
        
        // 为每个身体部位设置温度对应的纹理坐标
        // 头部 - 使用temperatures[0]
        float headTemp = normalizeTemperature(temperatures[0]);
        for (int i = 0; i < 6; i++) {
            texCoords[i*2] = headTemp;
            texCoords[i*2+1] = 0.5f;
        }
        
        // 躯干 - 使用temperatures[1]
        float torsoTemp = normalizeTemperature(temperatures[1 * 4]);
        for (int i = 6; i < 12; i++) {
            texCoords[i*2] = torsoTemp;
            texCoords[i*2+1] = 0.5f;
        }
        
        // 左臂 - 使用temperatures[2]
        float leftArmTemp = normalizeTemperature(temperatures[2 * 4]);
        for (int i = 12; i < 18; i++) {
            texCoords[i*2] = leftArmTemp;
            texCoords[i*2+1] = 0.5f;
        }
        
        // 右臂 - 使用temperatures[3]
        float rightArmTemp = normalizeTemperature(temperatures[3 * 4]);
        for (int i = 18; i < 24; i++) {
            texCoords[i*2] = rightArmTemp;
            texCoords[i*2+1] = 0.5f;
        }
        
        // 左腿 - 使用temperatures[4]
        float leftLegTemp = normalizeTemperature(temperatures[4 * 4]);
        for (int i = 24; i < 30; i++) {
            texCoords[i*2] = leftLegTemp;
            texCoords[i*2+1] = 0.5f;
        }
        
        // 右腿 - 使用temperatures[5]
        float rightLegTemp = normalizeTemperature(temperatures[5 * 4]);
        for (int i = 30; i < 36; i++) {
            texCoords[i*2] = rightLegTemp;
            texCoords[i*2+1] = 0.5f;
        }
        
        // 更新纹理坐标缓冲区
        texCoordBuffer.position(0);
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
        
        System.out.println("纹理坐标已更新 - 头部:" + headTemp + 
                          ", 躯干:" + torsoTemp + 
                          ", 左臂:" + leftArmTemp + 
                          ", 右臂:" + rightArmTemp + 
                          ", 左腿:" + leftLegTemp + 
                          ", 右腿:" + rightLegTemp);
    }
    
    // 将温度值归一化到0-1范围
    private float normalizeTemperature(float temperature) {
        // 假设温度范围在35-42度之间
        float normalizedTemp = (temperature - 35.0f) / 7.0f;
        // 限制在0-1范围内
        return Math.max(0.0f, Math.min(1.0f, normalizedTemp));
    }
}