package com.lian.petadoption.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleLineChartView extends View {

    // 画笔
    private Paint axisPaint;   // 坐标轴画笔
    private Paint gridPaint;   // 网格线画笔
    private Paint textPaint;   // 文字画笔
    private Paint linePaint;   // 折线画笔
    private Paint dotPaint;    // 数据点画笔

    // 数据
    private List<Map<String, Object>> dataList = new ArrayList<>();

    // 尺寸配置 (单位: px，在 init 中计算)
    private float paddingLeft, paddingBottom, paddingTop, paddingRight;
    private float textOffset;

    // 颜色配置：申请(橙色)、成功(绿色)、新增用户(蓝色)
    private final int[] COLORS = {Color.parseColor("#FF9800"), Color.parseColor("#4CAF50"), Color.parseColor("#2196F3")};
    private final String[] KEYS = {"apply", "success", "user"};

    // 复用 Path 防止内存抖动
    private final Path[] paths = new Path[3];

    public SimpleLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // 初始化尺寸
        paddingLeft = dp2px(40);
        paddingBottom = dp2px(30);
        paddingTop = dp2px(30);
        paddingRight = dp2px(20);
        textOffset = dp2px(8);

        // 1. 坐标轴画笔
        axisPaint = new Paint();
        axisPaint.setColor(Color.parseColor("#CCCCCC"));
        axisPaint.setStrokeWidth(dp2px(1));
        axisPaint.setAntiAlias(true);

        // 2. 网格线画笔 (虚线)
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#EEEEEE"));
        gridPaint.setStrokeWidth(dp2px(1));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10f, 10f}, 0f));
        gridPaint.setAntiAlias(true);

        // 3. 文字画笔
        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#666666"));
        textPaint.setTextSize(sp2px(10)); // 10sp
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // 4. 折线画笔
        linePaint = new Paint();
        linePaint.setStrokeWidth(dp2px(2));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        // 圆角连接
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        // 5. 数据点画笔
        dotPaint = new Paint();
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);

        // 初始化 Path
        for (int i = 0; i < paths.length; i++) {
            paths[i] = new Path();
        }
    }

    public void setData(List<Map<String, Object>> data) {
        this.dataList = data;
        invalidate(); // 触发重绘
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataList == null || dataList.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        // 有效绘图区域
        float chartW = width - paddingLeft - paddingRight;
        float chartH = height - paddingTop - paddingBottom;

        // 1. 计算最大值 (Y轴量程)
        long maxY = calculateMaxY();

        // 2. 绘制 Y 轴刻度线和文字 (分5档)
        textPaint.setTextAlign(Paint.Align.RIGHT); // Y轴文字右对齐
        for (int i = 0; i <= 5; i++) {
            float y = (height - paddingBottom) - i * (chartH / 5);
            long value = i * (maxY / 5);

            // 绘制文字
            canvas.drawText(String.valueOf(value), paddingLeft - textOffset, y + dp2px(3), textPaint);

            // 绘制横向网格线 (除了X轴那一条用实线，其他用虚线)
            if (i > 0) {
                Path gridPath = new Path();
                gridPath.moveTo(paddingLeft, y);
                gridPath.lineTo(width - paddingRight, y);
                canvas.drawPath(gridPath, gridPaint);
            }
        }

        // 绘制 X 轴实线
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, axisPaint);
        // 绘制 Y 轴实线
        canvas.drawLine(paddingLeft, height - paddingBottom, paddingLeft, paddingTop, axisPaint);

        // 3. 绘制 X 轴刻度和折线
        int count = dataList.size();
        // 防止除以0
        float xStep = count > 1 ? chartW / (count - 1) : chartW / 2;

        textPaint.setTextAlign(Paint.Align.CENTER); // X轴文字居中

        // 重置路径
        for (Path p : paths) p.reset();

        // 遍历数据点
        for (int i = 0; i < count; i++) {
            float x = (count == 1) ? (paddingLeft + chartW / 2) : (paddingLeft + i * xStep);

            // 绘制底部日期
            Object dateObj = dataList.get(i).get("date");
            String date = dateObj != null ? dateObj.toString() : "";
            canvas.drawText(date, x, height - paddingBottom / 2, textPaint);

            // 计算三个指标的坐标并记录 Path
            for (int k = 0; k < KEYS.length; k++) {
                long val = getSafeLong(dataList.get(i).get(KEYS[k]));
                // 坐标转换：数值越大，Y越小
                float y = (height - paddingBottom) - (val / (float) maxY) * chartH;

                if (i == 0) {
                    paths[k].moveTo(x, y);
                } else {
                    paths[k].lineTo(x, y);
                }
            }
        }

        // 4. 真正绘制折线和点 (分开循环是为了让点盖在线上)
        for (int k = 0; k < KEYS.length; k++) {
            // 画线
            linePaint.setColor(COLORS[k]);
            canvas.drawPath(paths[k], linePaint);

            // 画点
            dotPaint.setColor(COLORS[k]);
            for (int i = 0; i < count; i++) {
                float x = (count == 1) ? (paddingLeft + chartW / 2) : (paddingLeft + i * xStep);
                long val = getSafeLong(dataList.get(i).get(KEYS[k]));
                float y = (height - paddingBottom) - (val / (float) maxY) * chartH;

                // 绘制外圈白点增强立体感
                dotPaint.setColor(Color.WHITE);
                canvas.drawCircle(x, y, dp2px(5), dotPaint);
                // 绘制内圈实心点
                dotPaint.setColor(COLORS[k]);
                canvas.drawCircle(x, y, dp2px(3), dotPaint);
            }
        }
    }

    /**
     * 计算 Y 轴最大值，保证留有顶部余量，且为 5 的倍数以便整除
     */
    private long calculateMaxY() {
        long max = 0;
        for (Map<String, Object> map : dataList) {
            for (String key : KEYS) {
                long val = getSafeLong(map.get(key));
                if (val > max) max = val;
            }
        }
        if (max == 0) return 5;

        // 向上取整逻辑，让最大值稍微大一点，且容易被5整除
        // 例如 max=12 -> 目标15; max=3 -> 目标5
        long target = max + (max / 5 + 1);
        while (target % 5 != 0) {
            target++;
        }
        return target;
    }

    /**
     * 安全获取长整型数据
     */
    private long getSafeLong(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // dp 转 px
    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // sp 转 px
    private float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}