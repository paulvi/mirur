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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "mirur.mirur-ui";

    private static Activator plugin;

    private static SelectionCache variableCache;

    private static SelectionModel selectionModel;

    private static Preferences preferences;

    private static RemoteAgentDeployer agentDeployer;

    private static StatisticsCollector statsCollector;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        preferences = new Preferences(InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID));

        PluginLogSupport.initializeLogger();

        statsCollector = new StatisticsCollector();
        agentDeployer = new RemoteAgentDeployer();
        variableCache = new SelectionCache();
        selectionModel = new SelectionModel();

        statsCollector.started();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        statsCollector.shuttingDown();

        plugin = null;
        preferences.flush();
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public static SelectionCache getVariableCache() {
        return variableCache;
    }

    public static SelectionModel getSelectionModel() {
        return selectionModel;
    }

    public static Preferences getPreferences() {
        return preferences;
    }

    public static RemoteAgentDeployer getAgentDeployer() {
        return agentDeployer;
    }

    public static StatisticsCollector getStatistics() {
        return statsCollector;
    }

    public static Image getCachedImage(String name) {
        ImageRegistry registry = getDefault().getImageRegistry();
        Image image = registry.get(name);
        if (image == null) {
            registry.put(name, getImageDescriptor(name));
            image = registry.get(name);
        }

        return image;
    }
}
