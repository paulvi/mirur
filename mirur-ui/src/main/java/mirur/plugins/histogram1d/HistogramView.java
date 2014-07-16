package mirur.plugins.histogram1d;

import mirur.core.Array1D;
import mirur.core.PrimitiveArray;
import mirur.plugins.Array1DPlot;
import mirur.plugins.DataPainter;
import mirur.plugins.DataPainterImpl;
import mirur.plugins.SimplePlugin1D;

import com.metsci.glimpse.canvas.GlimpseCanvas;
import com.metsci.glimpse.painter.info.SimpleTextPainter;
import com.metsci.glimpse.painter.info.SimpleTextPainter.HorizontalPosition;
import com.metsci.glimpse.painter.info.SimpleTextPainter.VerticalPosition;

public class HistogramView extends SimplePlugin1D {
    public HistogramView() {
        super("Histogram", null);
    }

    @Override
    public DataPainter install(GlimpseCanvas canvas, PrimitiveArray array) {
        final Array1D array1d = (Array1D) array;
        final HistogramPainter painter = createPainter(array1d);
        Array1DPlot plot = new Array1DPlot(painter, array1d) {
            @Override
            protected SimpleTextPainter createTitlePainter() {
                SimpleTextPainter painter = new HistogramBinTextPainter(axis);
                painter.setHorizontalPosition(HorizontalPosition.Left);
                painter.setVerticalPosition(VerticalPosition.Center);
                return painter;
            }

            @Override
            protected void setTitlePainterData(Array1D array) {
                ((HistogramBinTextPainter) titlePainter).setHistogramPainter(array1d, painter);
            }

            @Override
            protected void updateAxesBounds(Array1D array) {
                painter.autoAdjustAxisBounds(axis);
                System.out.println(axis.getAxisX());
                // TODO bug fix, should be fixed in next Glimpse
                axis.getAxisX().setMax(axis.getAxisX().getMax() + painter.getBinSize());
                System.out.println(axis.getAxisX());
            }
        };

        plot.setAxisSizeX(40);

        DataPainterImpl result = new DataPainterImpl(plot);
        result.addAxis(plot.getAxis());

        finalInstall(canvas, result);
        return result;
    }

    @Override
    protected HistogramPainter createPainter(Array1D array) {
        HistogramPainter painter = new HistogramPainter();
        painter.setData(array.toFloats());
        return painter;
    }
}