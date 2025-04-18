package com.aj.bodyheartmap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aj.bodyheartmap.view.BodyModel;
import com.aj.bodyheartmap.view.HeatMapCoordinateView;
import com.aj.bodyheartmap.view.HeatMapView;
import com.aj.bodyheartmap.view.OpenGlxyz;
import com.aj.bodyheartmap.R;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private HeatMapView heatMapView;
    private float[] temperatureData;
    private Handler handler;
    private Random random = new Random();
    private boolean isSimulating = false;
    private Runnable temperatureUpdater;

    // 身体部位索引
    private static final int HEAD = 0;


    // 当前选中的身体部位
    private volatile int selectedBodyPart = HEAD;
    // 在布局文件中添加一个SeekBar用于调节透明度
    // 在MainActivity中添加透明度控制
    private SeekBar alphaSeekBar;
    private TextView alphaTextView;
    private float currentAlpha = 0.7f;
    private float currentScale = 0.8f; // 默认缩放因子

    private HeatMapCoordinateView coordinateView;
    private OpenGlxyz openGlxyz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //坐标系
        coordinateView = findViewById(R.id.coordinateView);
        // 设置坐标系参数
        coordinateView.setCoordinateTextSize(30f); // 设置更大的文字以便清晰查看
        coordinateView.setGridSpacing(200);        // 设置网格间距为200像素

        //初始化OpenGlxyz视图
        openGlxyz = findViewById(R.id.xyz);
        if (openGlxyz != null) {
            openGlxyz.setAxisScale(2.0f); // 设置坐标轴为原来的2倍大
        }
        Log.d(TAG, "OpenGlxyz视图初始化完成");

        heatMapView = findViewById(R.id.heat_map_view);

        // 初始化温度数据（13个身体部位，每个部位4个点）
        // 初始化温度数据（每个部位只存储一个温度值）
        //[35,42]
        temperatureData = new float[]{
                39.8f, // 头部
                36.6f, // 颈部
                39.7f, // 上身
                36.8f, // 左肩膀
                36.9f, // 左臂
                35.0f, // 左手
                36.9f, // 右肩膀
                36.8f, // 右臂
                35.7f, // 右手
                36.6f, // 左腿
                35.5f, // 左脚
                36.6f, // 右腿
                35.7f  // 右脚
        };

        // 更新热力图
        heatMapView.updateTemperatureData(temperatureData,currentAlpha);

        // 设置模拟温度变化
        handler = new Handler(Looper.getMainLooper());

        Button btnSimulate = findViewById(R.id.btn_simulate);
        btnSimulate.setOnClickListener(v -> {
            if (isSimulating) {
                stopTemperatureSimulation();
                btnSimulate.setText("开始模拟");
                isSimulating = false;
            } else {
                startTemperatureSimulation();
                btnSimulate.setText("停止模拟");
                isSimulating = true;
            }
        });

        // 初始化透明度调节组件
        alphaSeekBar = findViewById(R.id.alphaSeekBar);
        alphaTextView = findViewById(R.id.alphaTextView);

        alphaSeekBar.setProgress((int) (currentAlpha * 100));
        alphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 将进度值转换为0.0-1.0的透明度
                currentAlpha = progress / 100.0f;
                alphaTextView.setText("透明度: " + progress + "%");

                heatMapView.updateGlAlpha(currentAlpha);

                // 更新热力图透明度
                updateSelectedAreaTemperature(temperatureData[selectedBodyPart], currentAlpha);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });



        // 设置温度调节滑块
        SeekBar seekBarTemp = findViewById(R.id.seek_bar_temp);
        TextView tvTempValue = findViewById(R.id.tv_temp_value);

        seekBarTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float temp = 35.0f + (progress / 100.0f) * 7.0f; // 35-42度范围
                tvTempValue.setText(String.format("%.1f°C", temp));

                if (fromUser) {
                    // 更新选定区域的温度
                    updateSelectedAreaTemperature(temp, currentAlpha);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 设置身体部位选择按钮
        setupBodyPartButtons();

        // 在onCreate方法中添加
        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> {
            currentScale += 0.01f;
            if (currentScale > 3.0f) {
                currentScale = 3.0f;
            }
            heatMapView.setScaleFactor(currentScale);
        });
        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> {
            currentScale -= 0.01f;
            if (currentScale < 0.1f) {
                currentScale = 0.1f;
            }
            heatMapView.setScaleFactor(currentScale);
        });
        Button btnTestScale = findViewById(R.id.btn_test_scale);
        if (btnTestScale != null) {
            btnTestScale.setOnClickListener(v -> {
                // 测试不同的缩放值
                heatMapView.testScaling();
            });
        }

        // 或者直接设置几个固定的缩放值
        Button btnScale1 = findViewById(R.id.btn_scale_1);
        Button btnScale2 = findViewById(R.id.btn_scale_2);
        Button btnScale3 = findViewById(R.id.btn_scale_3);

        if (btnScale1 != null) {
            btnScale1.setOnClickListener(v -> {
                heatMapView.setScaleFactor(0.3f);
                Toast.makeText(this, "缩放: 0.3", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnScale2 != null) {
            btnScale2.setOnClickListener(v -> {
                heatMapView.setScaleFactor(0.6f);
                Toast.makeText(this, "缩放: 0.6", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnScale3 != null) {
            btnScale3.setOnClickListener(v -> {
                heatMapView.setScaleFactor(1.0f);
                Toast.makeText(this, "缩放: 1.0", Toast.LENGTH_SHORT).show();
            });
        }
        // 在onCreate方法中添加位置控制按钮的监听器
        Button btnMoveUp = findViewById(R.id.btn_move_up);
        Button btnMoveDown = findViewById(R.id.btn_move_down);
        Button btnMoveLeft = findViewById(R.id.btn_move_left);
        Button btnMoveRight = findViewById(R.id.btn_move_right);
        
        // 设置移动步长
        final float moveStep = 0.01f;
        
        if (btnMoveUp != null) {
            btnMoveUp.setOnClickListener(v -> {
                heatMapView.moveUp(moveStep);
            });
        }
        
        if (btnMoveDown != null) {
            btnMoveDown.setOnClickListener(v -> {
                heatMapView.moveDown(moveStep);
            });
        }
        
        if (btnMoveLeft != null) {
            btnMoveLeft.setOnClickListener(v -> {
                heatMapView.moveLeft(moveStep);
            });
        }
        
        if (btnMoveRight != null) {
            btnMoveRight.setOnClickListener(v -> {
                heatMapView.moveRight(moveStep);
            });
        }
    }


    // 身体部位的中文名称
    private static final String[] BODY_PART_NAMES = BodyModel.BODY_PARTS;

    private void setupBodyPartButtons() {
        // 身体部位按钮ID数组 - 使用新添加的13个按钮
        int[] buttonIds = {
            R.id.btn_head, R.id.btn_neck, R.id.btn_chest, 
            R.id.btn_left_shoulder, R.id.btn_left_arm, R.id.btn_left_hand,
            R.id.btn_right_shoulder, R.id.btn_right_arm, R.id.btn_right_hand,
            R.id.btn_left_thigh, R.id.btn_left_leg, R.id.btn_right_thigh, R.id.btn_right_leg
        };
        
        // 按钮与BODY_PARTS的映射关系 - 现在是一一对应的
        int[] buttonToBodyPartMap = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
        };
        
        // 当前选中的身体部位索引
        final int[] selectedPartIndex = {0};
        
        // 为每个按钮设置点击监听器
        for (int i = 0; i < buttonIds.length; i++) {
            final int index = i;
            final int bodyPartIndex = buttonToBodyPartMap[i];
            Button button = findViewById(buttonIds[i]);
            if (button != null) {  // 确保按钮存在
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedBodyPart = index;  // 更新全局选中部位变量
                        selectedPartIndex[0] = bodyPartIndex;  // 使用映射后的索引
                        updateButtonHighlight(buttonIds, index);
                        
                        // 更新温度滑动条的值
                        updateTempSliderForSelectedPart();
                        
                        // 显示选中的身体部位名称
                        Toast.makeText(MainActivity.this, 
                                      "已选择: " + BODY_PART_NAMES[bodyPartIndex], 
                                      Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "按钮未找到: " + index);
            }
        }
        
        // 默认选中头部
        updateButtonHighlight(buttonIds, 0);
    }

    // 更新按钮高亮状态
    private void updateButtonHighlight(int[] buttonIds, int selectedIndex) {
        for (int i = 0; i < buttonIds.length; i++) {
            Button button = findViewById(buttonIds[i]);
            if (button != null) {
                if (i == selectedIndex) {
                    button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3F51B5")));
                } else {
                    button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#444444")));
                }
            }
        }
    }


    // 更新温度滑动条的值
    private void updateTempSliderForSelectedPart() {
        // 计算选中部位的平均温度
        float curTemp = temperatureData[selectedBodyPart];

        // 更新滑块和温度显示
        SeekBar seekBarTemp = findViewById(R.id.seek_bar_temp);
        TextView tvTempValue = findViewById(R.id.tv_temp_value);

        int progress = (int) ((curTemp - 35.0f) / 7.0f * 100);
        seekBarTemp.setProgress(progress);
        tvTempValue.setText(String.format("%.1f°C", curTemp));
    }

    private void startTemperatureSimulation() {
        // 模拟温度变化
        temperatureUpdater = new Runnable() {
            @Override
            public void run() {
                // 随机更新温度数据
                for (int i = 0; i < temperatureData.length; i++) {
                    // 在原温度基础上随机波动 ±0.3 度
                    temperatureData[i] += (random.nextFloat() - 0.5f) * 0.6f;
                    // 限制温度范围在 35-42 度
                    temperatureData[i] = Math.max(35.0f, Math.min(42.0f, temperatureData[i]));
                }

                // 更新热力图
                heatMapView.updateTemperatureData(temperatureData,currentAlpha);

                // 如果当前有选中的身体部位，更新滑块 显示温度。
                updateTempSliderForSelectedPart();

                // 继续模拟
                handler.postDelayed(this, 1000); // 每秒更新一次
            }
        };

        handler.post(temperatureUpdater);
    }

    private void stopTemperatureSimulation() {
        if (temperatureUpdater != null) {
            handler.removeCallbacks(temperatureUpdater);
        }
    }

    private void updateSelectedAreaTemperature(float temperature, float alpha) {
        // 更新特定区域的温度
        temperatureData[selectedBodyPart] = temperature;

        // 更新热力图
        heatMapView.updateTemperatureData(temperatureData, alpha);
    }

    @Override
    protected void onPause() {
        super.onPause();
        heatMapView.onPause();
        // 暂停OpenGlxyz
        if (openGlxyz != null) {
            openGlxyz.onPause();
        }
        stopTemperatureSimulation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        heatMapView.onResume();
        if (isSimulating) {
            startTemperatureSimulation();
        }
    }

    // 设置热力图缩放
    private void setHeatMapScale(float scale) {
        if (heatMapView != null) {
            heatMapView.setScaleFactor(scale);
        }
    }
}
