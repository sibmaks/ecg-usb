package xyz.dma.ecg_usb;

import android.content.Context;
import android.util.AttributeSet;
import com.github.mikephil.charting.charts.LineChart;

public class FxLineChart extends LineChart {

    public FxLineChart(Context context) {
        super(context);
    }

    public FxLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FxLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new FxLineChartRenderer(this, mAnimator, mViewPortHandler);
    }
}
