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
package mirur.core;

public abstract class SimpleArrayVisitor extends AbstractArrayVisitor {
    @Override
    public void visit(int i, int j, long v) {
        visit(i, j, (double) v);
    }

    @Override
    public void visit(int i, int j, float v) {
        visit(i, j, (double) v);
    }

    @Override
    public void visit(int i, int j, int v) {
        visit(i, j, (double) v);
    }

    @Override
    public void visit(int i, int j, short v) {
        visit(i, j, (double) v);
    }

    @Override
    public void visit(int i, int j, char v) {
        visit(i, j, (double) v);
    }

    @Override
    public void visit(int i, int j, byte v) {
        visit(i, j, (double) v);
    }

    @Override
    public void visit(int i, int j, boolean v) {
        visit(i, j, v ? 1.0 : 0.0);
    }
}
