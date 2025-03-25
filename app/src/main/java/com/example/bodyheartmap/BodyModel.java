package com.example.bodyheartmap;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BodyModel {
    private static final String TAG = "BodyModel";
    
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    
    // 身体各部位的顶点索引范围
    private Map<String, int[]> bodyPartIndices;
    
    // 身体部位名称
//    private static final String[] BODY_PARTS = {
//        "head", "neck", "chest", "abdomen", "leftShoulder", "leftArm",
//        "leftHand", "rightShoulder", "rightArm", "rightHand",
//        "leftThigh", "leftLeg", "rightThigh", "rightLeg"
//    };
    
    private static final String[] BODY_PARTS = {
//        "头部", "颈部", "上身", "abdomen", "leftShoulder", "leftArm",
        "头部", "颈部", "上身",  "左肩膀", "左臂",
        "左手", "右肩膀", "右臂", "右手",
        "左腿", "左脚", "右腿", "右脚"
    };

    // 总顶点数 - 将在加载坐标后确定
    private int totalVertices = 0;
    
    // 构造函数
    public BodyModel(Context context) {
        loadBodyPartsFromAssets(context);
    }
    
    // 从assets加载身体部位坐标
    private void loadBodyPartsFromAssets(Context context) {
        Map<String, List<float[]>> bodyPartsCoordinates = new HashMap<>();
        
        // 初始化身体部位索引映射
        bodyPartIndices = new HashMap<>();
        
        // 为每个身体部位加载坐标
        for (String part : BODY_PARTS) {
            try {
                List<float[]> coordinates = loadCoordinatesFromAsset(context, part + ".json");
                bodyPartsCoordinates.put(part, coordinates);
                totalVertices += coordinates.size();
            } catch (IOException e) {
                Log.e(TAG, "无法加载身体部位坐标: " + part, e);
            }
        }
        
        // 创建顶点和纹理坐标缓冲区
        setupBuffers(bodyPartsCoordinates);
    }
    
    // 从资源文件加载坐标
    private List<float[]> loadCoordinatesFromAsset(Context context, String filename) throws IOException {
        List<float[]> coordinates = new ArrayList<>();
        InputStream is = context.getAssets().open(filename);
        
        try {
            // 读取JSON文件内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            
            // 解析JSON数组
            JSONArray jsonArray = new JSONArray(jsonString.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray pointArray = jsonArray.getJSONArray(i);
                if (pointArray.length() >= 2) {
                    float x = (float) pointArray.getDouble(0);
                    float y = (float) pointArray.getDouble(1);
                    
                    // 坐标归一化处理（可选，取决于您的坐标系统）
                    // 假设原始坐标范围是0-1000，转换为-1到1的OpenGL坐标系
                    float normalizedX = (x / 500.0f) - 1.0f;
                    float normalizedY = 1.0f - (y / 500.0f); // Y轴方向通常需要翻转
                    
                    coordinates.add(new float[]{normalizedX, normalizedY, 0.0f});
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析JSON文件失败: " + filename, e);
            throw new IOException("JSON解析错误", e);
        } finally {
            is.close();
        }
        
        return coordinates;
    }
    
    // 设置顶点和纹理坐标缓冲区
    private void setupBuffers(Map<String, List<float[]>> bodyPartsCoordinates) {
        // 创建顶点数组
        float[] vertices = new float[totalVertices * 3]; // 每个顶点3个坐标(x,y,z)
        float[] texCoords = new float[totalVertices * 2]; // 每个顶点2个纹理坐标(s,t)
        
        int vertexOffset = 0;
        int indexOffset = 0;
        
        // 为每个身体部位设置顶点和纹理坐标
        for (String part : BODY_PARTS) {
            List<float[]> coordinates = bodyPartsCoordinates.get(part);
            if (coordinates == null) {
                continue;
            }
            
            // 记录该部位的起始索引和顶点数量
            int startIndex = indexOffset;
            int vertexCount = coordinates.size();
            bodyPartIndices.put(part, new int[]{startIndex, vertexCount});
            
            // 复制顶点坐标
            for (float[] coord : coordinates) {
                vertices[vertexOffset++] = coord[0];
                vertices[vertexOffset++] = coord[1];
                vertices[vertexOffset++] = coord[2];
                
                // 设置默认纹理坐标 - 温度值0.5，透明度1.0
                texCoords[indexOffset * 2] = 0.5f;
                texCoords[indexOffset * 2 + 1] = 1.0f;
                
                indexOffset++;
            }
        }
        
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
        
        Log.i(TAG, "身体模型初始化完成，总顶点数: " + totalVertices);
    }
    
    // 更新纹理坐标 - 支持13个身体部位的温度和透明度
    public void updateTextureCoordinates(float[] temperatures, float globalAlpha) {
        if (temperatures == null || temperatures.length < BODY_PARTS.length) {
            Log.e(TAG, "温度数据不足，需要至少 " + BODY_PARTS.length + " 个值");
            return;
        }
        
        // 创建新的纹理坐标数组
        float[] texCoords = new float[totalVertices * 2];
        
        // 为每个身体部位设置温度和透明度
        for (int i = 0; i < BODY_PARTS.length; i++) {
            String part = BODY_PARTS[i];
            int[] indices = bodyPartIndices.get(part);
            if (indices == null) {
                continue;
            }
            
            int startIndex = indices[0];
            int vertexCount = indices[1];
            float normalizedTemp = normalizeTemperature(temperatures[i]);
            
            // 为该部位的所有顶点设置相同的温度和透明度
            for (int j = 0; j < vertexCount; j++) {
                int index = startIndex + j;
                texCoords[index * 2] = normalizedTemp;
                texCoords[index * 2 + 1] = globalAlpha;
            }
        }
        
        // 更新纹理坐标缓冲区
        texCoordBuffer.position(0);
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
        
        Log.i(TAG, "纹理坐标已更新，全局透明度: " + globalAlpha);
    }
    
    // 将温度值归一化到0-1范围
    private float normalizeTemperature(float temperature) {
        // 假设温度范围在35-42度之间
        float normalizedTemp = (temperature - 35.0f) / 7.0f;
        // 限制在0-1范围内
        return Math.max(0.0f, Math.min(1.0f, normalizedTemp));
    }
    
    // Getter方法
    public FloatBuffer getVertexBuffer() {
        return vertexBuffer;
    }
    
    public FloatBuffer getTexCoordBuffer() {
        return texCoordBuffer;
    }
    
    public Map<String, int[]> getBodyPartIndices() {
        return bodyPartIndices;
    }
    
    public int getTotalVertices() {
        return totalVertices;
    }
}