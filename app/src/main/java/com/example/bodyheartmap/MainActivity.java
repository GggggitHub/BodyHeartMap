package com.example.bodyheartmap;

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

import com.aj.bodyheartmap.R;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private HeatMapView heatMapView;
    private float[] temperatureData;
    private float currentTemperature = 36.5f;
    private Handler handler;
    private Random random = new Random();
    private boolean isSimulating = false;
    private Runnable temperatureUpdater;

    // 身体部位索引
    private static final int HEAD = 0;
    private static final int TORSO = 1;
    private static final int LEFT_ARM = 2;
    private static final int RIGHT_ARM = 3;
    private static final int LEFT_LEG = 4;
    private static final int RIGHT_LEG = 5;

    //各部位的名称
    public static final String[] bodyPartNames = {
            "头部", "躯干", "左臂", "右臂", "左腿", "右腿"
    };

    // 当前选中的身体部位
    private volatile int selectedBodyPart = TORSO;
    // 在布局文件中添加一个SeekBar用于调节透明度
    // 在MainActivity中添加透明度控制
    private SeekBar alphaSeekBar;
    private TextView alphaTextView;
    private float currentAlpha = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        heatMapView = findViewById(R.id.heat_map_view);

        // 初始化温度数据（示例数据）
        temperatureData = new float[]{
                36.5f, 36.6f, 36.7f, 36.5f, // 头部
                36.7f, 36.9f, 37.1f, 36.8f, // 躯干
                36.6f, 36.4f, 36.5f, 36.7f, // 左臂
                36.7f, 36.5f, 36.6f, 36.8f, // 右臂
                36.3f, 36.4f, 36.6f, 36.5f, // 左腿
                36.4f, 36.5f, 36.7f, 36.6f  // 右腿
        };

        // 更新热力图
        heatMapView.updateTemperatureData(temperatureData);

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

        alphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 将进度值转换为0.0-1.0的透明度
                currentAlpha = progress / 100.0f;
                alphaTextView.setText("透明度: " + progress + "%");

                // 更新热力图透明度
                updateSelectedAreaTemperature(currentTemperature, currentAlpha);
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
                float temp = currentTemperature = 35.0f + (progress / 100.0f) * 7.0f; // 35-42度范围
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
    }

    private void setupBodyPartButtons() {
        Button btnHead = findViewById(R.id.btn_head);
        Button btnTorso = findViewById(R.id.btn_torso);
        Button btnLeftArm = findViewById(R.id.btn_left_arm);
        Button btnRightArm = findViewById(R.id.btn_right_arm);
        Button btnLeftLeg = findViewById(R.id.btn_left_leg);
        Button btnRightLeg = findViewById(R.id.btn_right_leg);

        View.OnClickListener bodyPartClickListener = v -> {
            int id = v.getId();

            if (id == R.id.btn_head) {
                selectedBodyPart = HEAD;
                Toast.makeText(this, "已选择头部", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_torso) {
                selectedBodyPart = TORSO;
                Toast.makeText(this, "已选择躯干", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_left_arm) {
                selectedBodyPart = LEFT_ARM;
                Toast.makeText(this, "已选择左臂", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_right_arm) {
                selectedBodyPart = RIGHT_ARM;
                Toast.makeText(this, "已选择右臂", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_left_leg) {
                selectedBodyPart = LEFT_LEG;
                Toast.makeText(this, "已选择左腿", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_right_leg) {
                selectedBodyPart = RIGHT_LEG;
                Toast.makeText(this, "已选择右腿", Toast.LENGTH_SHORT).show();
            }

            // 更新滑块值为当前选中部位的平均温度
            updateTempSliderForSelectedPart();
        };

        btnHead.setOnClickListener(bodyPartClickListener);
        btnTorso.setOnClickListener(bodyPartClickListener);
        btnLeftArm.setOnClickListener(bodyPartClickListener);
        btnRightArm.setOnClickListener(bodyPartClickListener);
        btnLeftLeg.setOnClickListener(bodyPartClickListener);
        btnRightLeg.setOnClickListener(bodyPartClickListener);
    }

    private void updateTempSliderForSelectedPart() {
        // 计算选中部位的平均温度
        float avgTemp = 0;
        int startIdx = selectedBodyPart * 4;
        for (int i = 0; i < 4; i++) {
            avgTemp += temperatureData[startIdx + i];
        }
        avgTemp /= 4;

        // 更新滑块和温度显示
        SeekBar seekBarTemp = findViewById(R.id.seek_bar_temp);
        TextView tvTempValue = findViewById(R.id.tv_temp_value);

        int progress = (int) ((avgTemp - 35.0f) / 5.0f * 100);
        seekBarTemp.setProgress(progress);
        tvTempValue.setText(String.format("%.1f°C", avgTemp));
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
                heatMapView.updateTemperatureData(temperatureData);

                // 如果当前有选中的身体部位，更新滑块
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
        Log.e(TAG, "updateSelectedAreaTemperature: 更新的区域是" + bodyPartNames[selectedBodyPart] + "      温度是" + temperature + "度");
        // 更新特定区域的温度
        int startIdx = selectedBodyPart * 4;
        for (int i = 0; i < 4; i++) {
            temperatureData[startIdx + i] = temperature;
        }

        // 更新热力图
        heatMapView.updateTemperatureData(temperatureData, alpha);
    }

    @Override
    protected void onPause() {
        super.onPause();
        heatMapView.onPause();
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

}