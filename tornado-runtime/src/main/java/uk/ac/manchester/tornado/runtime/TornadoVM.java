/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2013-2023, APT Group, Department of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package uk.ac.manchester.tornado.runtime;

import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.common.Event;
import uk.ac.manchester.tornado.api.profiler.TornadoProfiler;
import uk.ac.manchester.tornado.runtime.common.Tornado;
import uk.ac.manchester.tornado.runtime.common.TornadoLogger;
import uk.ac.manchester.tornado.runtime.graph.TornadoExecutionContext;
import uk.ac.manchester.tornado.runtime.graph.TornadoGraph;
import uk.ac.manchester.tornado.runtime.graph.TornadoVMBytecodeBuilder;
import uk.ac.manchester.tornado.runtime.graph.TornadoVMGraphCompiler;
import uk.ac.manchester.tornado.runtime.interpreter.TornadoVMInterpreter;
import uk.ac.manchester.tornado.runtime.tasks.TornadoTaskGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * * There is an instance of the {@link TornadoVM} per {@link TornadoTaskGraph}. * Each TornadoVM contains the logic to orchestrate the execution on the * parallel device (e.g., a GPU).
 */
public class TornadoVM extends TornadoLogger {
    private final TornadoExecutionContext graphContext;
    private final boolean setNewDevice;

    private double totalTime;
    private long invocations;
    private final TornadoProfiler timeProfiler;

    private final TornadoVMBytecodeBuilder[] tornadoVMBytecodes;

    private final TornadoVMInterpreter[] tornadoVMInterpreters;

    /**
     * Constructs a new TornadoVM instance.
     *
     * @param graphContext
     *         the TornadoExecutionContext for managing the execution context
     * @param tornadoGraph
     *         the TornadoGraph representing the TaskGraph
     * @param timeProfiler
     *         the TornadoProfiler for profiling execution time
     * @param batchSize
     *         the batch size when running in batch mode
     */
    public TornadoVM(TornadoExecutionContext graphContext, TornadoGraph tornadoGraph, TornadoProfiler timeProfiler, long batchSize, boolean setNewDevice) {
        this.graphContext = graphContext;
        this.timeProfiler = timeProfiler;
        this.setNewDevice = setNewDevice;
        totalTime = 0;
        invocations = 0;
        tornadoVMBytecodes = TornadoVMGraphCompiler.compile(tornadoGraph, graphContext, batchSize);
        tornadoVMInterpreters = new TornadoVMInterpreter[validContextsSize(graphContext)];
        bindBytecodesToInterpreters();
    }

    /**
     * Calculates the number of valid contexts in the provided TornadoExecutionContext. A valid context refers to a context that is not null within the list of devices. We do this cause in the
     * ExecutionContext we don't append devices sequentially, but we place them in the order/index that they are in the driver.
     *
     * @param graphContext
     *         The TornadoExecutionContext to calculate the valid contexts for.
     * @return The number of valid contexts in the TornadoExecutionContext.
     */
    private int validContextsSize(TornadoExecutionContext graphContext) {
        // Count the number of null devices in the context
        int nullDevicesEntries = (int) graphContext.getDevices().stream().filter(device -> device == null).count();

        // Calculate the number of valid contexts by subtracting the number of null devices from the total number of devices
        return graphContext.getDevices().size() - nullDevicesEntries;
    }

    private void bindBytecodesToInterpreters() {
        int indexOfValidContex;
        for (int i = 0; i < validContextsSize(graphContext); i++) {
            indexOfValidContex = validContextsSize(graphContext) < graphContext.getDevices().size() ? i + 1 : i;
            tornadoVMInterpreters[i] = new TornadoVMInterpreter(graphContext, tornadoVMBytecodes[indexOfValidContex], timeProfiler, graphContext.getDevices().get(i));
        }
    }

    public TornadoVMBytecodeBuilder[] getTornadoVMBytecodes() {
        return tornadoVMBytecodes;
    }

    public Event execute() {
        return runInParallel() ? executeInterpretersMultithreaded() : executeInterpretersSingleThreaded();
    }

    private Event executeInterpretersSingleThreaded() {
        for (int i = 0; i < validContextsSize(graphContext); i++) {
            tornadoVMInterpreters[i].execute(false);
        }
        return new EmptyEvent();
    }

    private Event executeInterpretersMultithreaded() {
        // Create a thread pool with a fixed number of threads
        int numThreads = graphContext.getDevices().size();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Create a list to hold the futures of each execution
        List<Future<?>> futures = new ArrayList<>();

        // Submit each task to the thread pool
        for (TornadoVMInterpreter tornadoVMInterpreter : tornadoVMInterpreters) {
            Future<?> future = executor.submit(() -> tornadoVMInterpreter.execute(false));
            futures.add(future);
        }
        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get(); // Blocking call to wait for the task to complete
            } catch (Exception e) {
                // Handle any exceptions that occurred during execution
                e.printStackTrace();
            }
        }
        // Shutdown the executor after all tasks have completed

        executor.shutdown();
        return new EmptyEvent();
    }

    private boolean runInParallel() {
        return Tornado.PARALLEL_INTERPRETERS && (graphContext.getDevices().size() > 1);
    }

    public void executeActionOnInterpreters(Consumer<TornadoVMInterpreter> action) {
        for (TornadoVMInterpreter tornadoVMInterpreter : tornadoVMInterpreters) {
            action.accept(tornadoVMInterpreter);
        }
    }

    public void clearInstalledCode() {
        executeActionOnInterpreters(TornadoVMInterpreter::clearInstalledCode);
    }

    public void setCompileUpdate() {
        executeActionOnInterpreters(TornadoVMInterpreter::setCompileUpdate);
    }

    public void dumpProfiles() {
        executeActionOnInterpreters(TornadoVMInterpreter::dumpProfiles);
    }

    public void dumpEvents() {
        executeActionOnInterpreters(TornadoVMInterpreter::dumpEvents);
    }

    public void clearProfiles() {
        executeActionOnInterpreters(TornadoVMInterpreter::clearProfiles);
    }

    public void printTimes() {
        executeActionOnInterpreters(TornadoVMInterpreter::printTimes);
    }

    public void warmup() {
        executeActionOnInterpreters(TornadoVMInterpreter::warmup);
    }

    public void fetchGlobalStates() {
        executeActionOnInterpreters(TornadoVMInterpreter::fetchGlobalStates);
    }

    public void setGridScheduler(GridScheduler gridScheduler) {
        for (TornadoVMInterpreter interpreter : tornadoVMInterpreters) {
            interpreter.setGridScheduler(gridScheduler);
        }
    }

}