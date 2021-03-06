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

import static com.metsci.glimpse.util.logging.LoggerUtils.logFine;
import static mirur.plugin.Activator.getStatistics;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;

public class ReceiveArrayJob extends Job {
    private static final Logger LOGGER = Logger.getLogger(ReceiveArrayJob.class.getName());

    private final String name;
    private final IJavaVariable var;
    private final IJavaStackFrame frame;
    private final IJavaClassType agentType;

    public ReceiveArrayJob(String name, IJavaVariable var, IJavaStackFrame frame, IJavaClassType agentType) {
        super("Receiving array from agent");
        this.name = name;
        this.var = var;
        this.frame = frame;
        this.agentType = agentType;

        setPriority(SHORT);
        setSystem(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            invokeAgent(agentType);
            return Status.OK_STATUS;
        } catch (Exception ex) {
            Activator.getSelectionModel().select(null);
            return new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Unexpected exception transfering array data from agent", ex);
        }
    }

    private void invokeAgent(IJavaClassType agentType) throws Exception {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(null);
        int port = serverSocket.getLocalPort();

        CyclicBarrier barrier = new CyclicBarrier(2);

        logFine(LOGGER, "Waiting to receive array on port %d", port);

        FutureTask<Object> socketTask = new FutureTask<>(new IncomingConnectionTask(barrier, serverSocket));
        Thread waitingThread = new Thread(socketTask, "mirur-socket-listen");
        waitingThread.setDaemon(true);
        waitingThread.start();

        // TODO I really don't like this method of waiting until the socket is listening
        barrier.await();

        IJavaDebugTarget target = (IJavaDebugTarget) frame.getDebugTarget();
        IJavaValue portValue = target.newValue(port);
        IJavaValue value = (IJavaValue) var.getValue();
        IJavaValue[] args = new IJavaValue[] { value, portValue };
        agentType.sendMessage("sendAsArray", "(Ljava/lang/Object;I)V", args, (IJavaThread) frame.getThread());
        logFine(LOGGER, "Called MirurAgent.sendAsArray(Object, int) successfully");

        Object arrayObject = socketTask.get();
        getStatistics().transformedViaAgent(var.getGenericSignature());

        new SubmitArrayToUIJob(name, var, frame, arrayObject).schedule();
    }

    private class IncomingConnectionTask implements Callable<Object> {
        final CyclicBarrier barrier;
        final ServerSocket serverSocket;

        IncomingConnectionTask(CyclicBarrier barrier, ServerSocket serverSocket) {
            this.barrier = barrier;
            this.serverSocket = serverSocket;
        }

        @Override
        public Object call() throws Exception {
            try {
                barrier.await();
                Socket sock = serverSocket.accept();

                InputStream in = sock.getInputStream();
                ObjectInputStream objIn = new ObjectInputStream(in);
                Object value = objIn.readObject();
                objIn.close();

                return value;
            } finally {
                serverSocket.close();
            }
        }
    }
}
