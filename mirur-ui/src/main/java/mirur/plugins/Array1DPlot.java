package mirur.plugins;

import mirur.core.Array1D;
import mirur.core.MinMaxValueVisitor;
import mirur.core.VisitArray;

import com.metsci.glimpse.painter.base.GlimpseDataPainter2D;
import com.metsci.glimpse.painter.info.SimpleTextPainter;
import com.metsci.glimpse.painter.info.SimpleTextPainter.HorizontalPosition;
import com.metsci.glimpse.painter.info.SimpleTextPainter.VerticalPosition;
import com.metsci.glimpse.plot.SimplePlot2D;

public class Array1DPlot extends SimplePlot2D {
    private ShaderWrapperPainter shaderWrapper;

    public Array1DPlot(GlimpseDataPainter2D dataPainter, Array1D array) {
        setAxisSizeX(25);
        setTitleHeight(30);

        setData(array);

        shaderWrapper = new ShaderWrapperPainter();
        addPainter(dataPainter, shaderWrapper, DATA_LAYER);
    }

    @Override
    protected void initializePainters() {
        super.initializePainters();

        getCrosshairPainter().showSelectionBox(false);
        titlePainter.setHorizontalPosition(HorizontalPosition.Left);
    }

    @Override
    protected SimpleTextPainter createTitlePainter() {
        SimpleTextPainter painter = new Array1DTitlePainter(getAxis());
        painter.setHorizontalPosition(HorizontalPosition.Left);
        painter.setVerticalPosition(VerticalPosition.Center);
        return painter;
    }

    public void setShaders(InitializablePipeline pipeline) {
        shaderWrapper.setPipeline(pipeline);
    }

    protected void setTitlePainterData(Array1D array) {
        ((Array1DTitlePainter) titlePainter).setArray(array);
    }

    protected void updateAxesBounds(Array1D array) {
        MinMaxValueVisitor minMaxVisitor = VisitArray.visit(array.getData(), new MinMaxValueVisitor());
        double min = minMaxVisitor.getMin();
        double max = minMaxVisitor.getMax();

        if (min == max) {
            max++;
        }

        getAxisX().setMin(0);
        getAxisX().setMax(array.getSize());
        getAxisY().setMin(min);
        getAxisY().setMax(max);
    }

    protected void setData(Array1D array) {
        updateAxesBounds(array);
        setTitlePainterData(array);
    }
}
