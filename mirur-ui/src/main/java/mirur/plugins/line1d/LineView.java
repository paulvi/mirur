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
package mirur.plugins.line1d;

import mirur.core.Array1D;
import mirur.plugins.SimplePlugin1D;

import com.metsci.glimpse.painter.base.GlimpseDataPainter2D;

public class LineView extends SimplePlugin1D {
    public LineView() {
        super("Line", null);
    }

    @Override
    protected GlimpseDataPainter2D createPainter(Array1D array) {
        LinePainter painter = new LinePainter();
        painter.setData(array);
        return painter;
    }
}
