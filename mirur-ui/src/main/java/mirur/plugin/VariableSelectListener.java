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
package mirur.plugin;

import static com.metsci.glimpse.util.logging.LoggerUtils.logWarning;
import static mirur.plugin.Activator.getSelectionModel;
import static mirur.plugin.Activator.getVariableCache;

import java.util.Arrays;
import java.util.logging.Logger;

import mirur.core.PrimitiveArray;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.views.variables.IndexedVariablePartition;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.internal.debug.core.model.JDIArrayEntryVariable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.metsci.glimpse.util.StringUtils;

@SuppressWarnings("restriction")
public class VariableSelectListener implements ISelectionListener, INullSelectionListener, IDebugEventSetListener, IDebugContextListener {
    private static final Logger LOGGER = Logger.getLogger(VariableSelectListener.class.getName());

    private static final String VARIABLE_VIEW_ID = "org.eclipse.debug.ui.VariableView";

    public void install(IWorkbenchWindow window) {
        DebugPlugin.getDefault().addDebugEventListener(this);

        IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(window);
        service.addDebugContextListener(this);

        window.getSelectionService().addPostSelectionListener(VARIABLE_VIEW_ID, this);
    }

    public void uninstall(IWorkbenchWindow window) {
        DebugPlugin.getDefault().removeDebugEventListener(this);

        IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(window);
        service.removeDebugContextListener(this);

        window.getSelectionService().removePostSelectionListener(VARIABLE_VIEW_ID, this);

        getVariableCache().clear();
    }

    private void update() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(window);
        ISelection contextSelection = service.getActiveContext();
        IJavaStackFrame frame = extract(contextSelection, IJavaStackFrame.class);

        IVariable variable = null;
        IViewPart view = window.getActivePage().findView(VARIABLE_VIEW_ID);
        String name = "Mirur Object";
        if (view instanceof AbstractDebugView) {
            ISelection varSelection = ((AbstractDebugView) view).getViewer().getSelection();
            variable = extract(varSelection, IVariable.class);

            if (variable != null && varSelection instanceof ITreeSelection) {
                TreePath[] paths = ((ITreeSelection) varSelection).getPathsFor(variable);
                try {
                    name = getVariableName(paths[0]);
                } catch (DebugException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    logWarning(LOGGER, "Could not get name for %s", ex, varSelection);
                }
            }
        }

        // check a bunch of pre-conditions
        if (frame == null ||
                variable == null ||
                !(frame.getDebugTarget() instanceof IJavaDebugTarget) ||
                !frame.getThread().isSuspended() ||
                !isValidRefType(variable)) {
            getSelectionModel().select(null);
        } else {
            if (getVariableCache().contains(variable, frame)) {
                PrimitiveArray array = getVariableCache().getArray(variable, frame);
                getSelectionModel().select(array);
            } else {
                new SelectStrategyJob(name, variable, frame).schedule();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T extract(ISelection selection, Class<T> type) {
        if (selection instanceof IStructuredSelection) {
            Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            if (type.isInstance(firstElement)) {
                return (T) firstElement;
            }
        }

        return null;
    }

    private String getVariableName(TreePath path) throws DebugException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {

        String[] parts = new String[path.getSegmentCount()];
        Arrays.fill(parts, "");
        for (int i = path.getSegmentCount() - 1; i >= 0; i--) {
            IVariable var = (IVariable) path.getSegment(i);
            parts[i] = var.getName();

            if (var instanceof IJavaVariable && !(var instanceof JDIArrayEntryVariable)) {
                break;
            }
        }

        for (int i = 0; i < path.getSegmentCount(); i++) {
            IVariable var = (IVariable) path.getSegment(i);

            /*
             * Collapse when multiple partitions are expanded or a
             * partition is expanded into an entry.
             */
            if (var instanceof IndexedVariablePartition &&
                    i < path.getSegmentCount() - 1 &&
                    (path.getSegment(i + 1) instanceof IndexedVariablePartition ||
                            path.getSegment(i + 1) instanceof JDIArrayEntryVariable)) {
                parts[i] = "";
            }
        }

        return StringUtils.join("", parts);
    }

    private boolean isValidRefType(IVariable var) {
        /*
         * The thing can't be plotted if it's a primitive or void
         */
        try {
            return (var instanceof IJavaVariable && ((IJavaVariable) var).getJavaType() instanceof IJavaReferenceType)
                    || (var instanceof IndexedVariablePartition);
        } catch (DebugException ex) {
            return false;
        }
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        update();
    }

    @Override
    public void debugContextChanged(DebugContextEvent event) {
        if (event.getFlags() == DebugContextEvent.ACTIVATED) {
            update();
        }
    }

    @Override
    public void handleDebugEvents(DebugEvent[] events) {
        if (events.length == 1) {
            DebugEvent event = events[0];
            int kind = event.getKind();
            if ((kind == DebugEvent.RESUME && event.getDetail() == DebugEvent.EVALUATION_IMPLICIT) || kind == DebugEvent.SUSPEND) {
                // probably generating a .toString on the variable
            } else {
                Activator.getVariableCache().clear();
                if (kind == DebugEvent.TERMINATE && event.getSource() instanceof IJavaDebugTarget) {
                    Activator.getAgentDeployer().clear((IJavaDebugTarget) event.getSource());
                }
            }
        } else {
            for (DebugEvent e : events) {
                handleDebugEvents(new DebugEvent[] { e });
            }
        }
    }

    public void forceUpdateNotify() {
        update();
    }
}
