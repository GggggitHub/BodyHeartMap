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

/**
 * 人体模型类
 * 加载人体各部位的坐标数据，并绘制人体模型
 *
 JSON文件中的坐标范围 ：

 - 查看JSON文件中的坐标，如头部.json中的坐标范围约为(400-600, 180-450)
 - 左腿.json中的坐标范围约为(290-490, 1270-2250)
 - 这表明整个人体图像的坐标范围大约是(0-1000, 0-2500)
 归一化处理：
 - 假设您希望将这些坐标归一化到OpenGL的坐标系中，即(-1, -1)到(1, 1)
 - 归一化的公式为： normalizedX = (x / 500.0f) - 1.0f; normalizedY = 1.0f - (y / 500.0f);

 这里将Y轴进行了翻转，但基准值500可能与实际图像尺寸不匹配
 */
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

    // 添加边界坐标变量
    private float minX = Float.MAX_VALUE;
    private float maxX = Float.MIN_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxY = Float.MIN_VALUE;
    private float[] minXPoint = new float[2];
    private float[] maxXPoint = new float[2];
    private float[] minYPoint = new float[2];
    private float[] maxYPoint = new float[2];

    // 添加坐标跨度变量
    private float xSpan;
    private float ySpan;

    // 添加坐标偏移量
    private float xOffset;
    private float yOffset;

    // 构造函数
    public BodyModel(Context context) {
        loadBodyPartsFromAssets(context);
    }
    
    // 从assets加载身体部位坐标
    private void loadBodyPartsFromAssets(Context context) {
        Map<String, List<float[]>> bodyPartsCoordinates = new HashMap<>();

        try {
            // 首先加载完整的人体轮廓以计算边界
            String jsonStr = loadJSONFromAsset(context, "body_red_2_contour_copy.json");
            JSONArray jsonArray = new JSONArray(jsonStr);

            // 计算边界
            calculateBoundaries(jsonArray);

            // 计算坐标跨度
            xSpan = maxX - minX;
            ySpan = maxY - minY;

            // 计算偏移量，使左上角为(0,0)
            xOffset = minX;
            yOffset = minY;

            Log.d(TAG, "边界坐标: minX=" + minX + ", maxX=" + maxX + ", minY=" + minY + ", maxY=" + maxY);
            Log.d(TAG, "坐标跨度: xSpan=" + xSpan + ", ySpan=" + ySpan);
            Log.d(TAG, "坐标偏移量: xOffset=" + xOffset + ", yOffset=" + yOffset);

        } catch (Exception e) {
            Log.e(TAG, "解析JSON时出错", e);
        }



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

    private String loadJSONFromAsset(Context context, String s) {
        try {
            InputStream is = context.getAssets().open(s);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            return jsonString.toString();
        } catch (IOException e) {
           e.printStackTrace();
        }
        return null;
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
                    float x = (float) pointArray.getDouble(0) - xOffset;
                    float y = (float) pointArray.getDouble(1) - yOffset;

//                    coordinates.add(processVertexCoordinates(x,y,HeatMapRenderer.useNormalizedCoordinates));

                    // 坐标归一化处理（可选，取决于您的坐标系统）
                    // 假设原始坐标范围是0-1000，转换为-1到1的OpenGL坐标系
//                    float normalizedX = (x / 500.0f) - 1.0f;
//                    float normalizedY = 1.0f - (y / 500.0f); //500不对。不准确。


//                    float normalizedX = (x / xSpan) - 1.0f;
//                    float normalizedY = 1.0f - (y / ySpan); //变形了。。不可以。

//                    float max = Math.max(xSpan, ySpan);
                    float max = Math.max(xSpan, ySpan)/2;// 以模型高度一半作为标准化尺度，底部对齐到屏幕-1位置
                    float normalizedX = (x / max) - 1.0f;
                    float normalizedY = 1.0f - (y / max); //TODO Y轴方向通常需要翻转

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

    //不需要了。 在处理顶点坐标时，根据坐标系类型进行不同的处理
    private float[] processVertexCoordinates(float x, float y, boolean useNormalizedCoordinates) {
        if (useNormalizedCoordinates) {
            // 归一化处理，将坐标映射到[-1, 1]范围
            float normalizedX = (x / xSpan) * 2.0f - 1.0f;
            float normalizedY = (y / ySpan) * 2.0f - 1.0f;
            return new float[]{normalizedX, normalizedY, 0.0f};
        } else {
            // 像素坐标处理，直接使用偏移后的坐标
            return new float[]{x - xOffset, y - yOffset, 0.0f};
        }
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

    // 计算边界坐标
    private void calculateBoundaries(JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray point = jsonArray.getJSONArray(i);
            float x = 0;
            float y = 0;
            if (point.length() >= 2) {
              x = (float) point.getDouble(0);
              y = (float) point.getDouble(1);
            }else {
                continue;
            }

            // 更新最小X坐标
            if (x < minX) {
                minX = x;
                minXPoint[0] = x;
                minXPoint[1] = y;
            }

            // 更新最大X坐标
            if (x > maxX) {
                maxX = x;
                maxXPoint[0] = x;
                maxXPoint[1] = y;
            }

            // 更新最小Y坐标
            if (y < minY) {
                minY = y;
                minYPoint[0] = x;
                minYPoint[1] = y;
            }

            // 更新最大Y坐标
            if (y > maxY) {
                maxY = y;
                maxYPoint[0] = x;
                maxYPoint[1] = y;
            }
        }
    }

    // 获取边界信息的方法
    public float[] getBoundaries() {
        Log.d(TAG, "人体边界: minX=" + minX + ", maxX=" + maxX +
                ", minY=" + minY + ", maxY=" + maxY);
        return new float[]{minX, maxX, minY, maxY};
    }

    // 获取坐标跨度
    public float[] getSpan() {
        return new float[]{xSpan, ySpan};
    }

    // 获取坐标偏移量
    public float[] getOffset() {
        return new float[]{xOffset, yOffset};
    }
}