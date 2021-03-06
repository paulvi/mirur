/*
 * This file is part of Mirur.
 *
 * Mirur is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mirur is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mirur.  If not, see <http://www.gnu.org/licenses/>.
 */
package mirur.plugins.histogram1d;

import static com.metsci.glimpse.support.color.GlimpseColor.getBlack;
import it.unimi.dsi.fastutil.floats.Float2IntMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import mirur.plugin.painterview.MirurLAF;

import com.metsci.glimpse.axis.Axis2D;
import com.metsci.glimpse.context.GlimpseBounds;
import com.metsci.glimpse.support.settings.LookAndFeel;

public class HistogramPainter extends com.metsci.glimpse.painter.plot.HistogramPainter {
    private Float2IntMap counts;
    private float[] borderColor = getBlack();

    @Override
    public void setLookAndFeel(LookAndFeel laf) {
        super.setLookAndFeel(laf);
        setColor(laf.getColor(MirurLAF.DATA_COLOR));
        borderColor = laf.getColor(MirurLAF.DATA_BORDER_COLOR);
    }

    public void setData(Float2IntMap counts, float binStart, float binSize) {
        this.binStart = binStart;
        this.counts = counts;
        setData(counts, binSize);
    }

    public int getCount(double binValue) {
        if (getMinX() <= binValue && binValue <= getMaxX()) {
            return counts.get((float) binValue);
        } else {
            return 0;
        }
    }

    public double getBin(double value) {
        return getBin(value, getBinSize(), getBinStart());
    }

    @Override
    public void paintTo(GL2 gl, GlimpseBounds bounds, Axis2D axis) {
        super.paintTo(gl, bounds, axis);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferHandle[0]);
        gl.glVertexPointer(2, GL.GL_FLOAT, 8, 0);
        gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);

        gl.glColor4fv(borderColor, 0);
        gl.glLineWidth(1);

        gl.glDrawArrays(GL.GL_LINES, 0, dataSize * 4);
    }
}