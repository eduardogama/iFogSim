/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd.apps;

import java.util.ArrayList;
import java.util.List;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Tuple;

/**
 * @author Eduardo S. Gama s Class responsible to implement the applications
 * that will be running in the simulations
 */
public class ApplicationModel {

    public Application createMasterWorkerApp(String appId, int brokerId) {
        Application app = Application.createApplication(appId, brokerId);

        app.addAppModule("MasterModule", 10);
        app.addAppModule("WorkerModule-1", 10);
        app.addAppModule("WorkerModule-2", 10);
        app.addAppModule("WorkerModule-3", 10);

        app.addAppEdge("Sensor", "MasterModule", 3000, 500, "Sensor", Tuple.UP, AppEdge.MODULE);
        app.addAppEdge("MasterModule", "WorkerModule-1", 100, 1000, "Task-1", Tuple.UP, AppEdge.MODULE);
        app.addAppEdge("MasterModule", "WorkerModule-2", 100, 1000, "Task-2", Tuple.UP, AppEdge.MODULE);
        app.addAppEdge("MasterModule", "WorkerModule-3", 100, 1000, "Task-3", Tuple.UP, AppEdge.MODULE);

        app.addAppEdge("WorkerModule-1", "MasterModule", 20, 50, "Response-1", Tuple.DOWN, AppEdge.MODULE);
        app.addAppEdge("WorkerModule-2", "MasterModule", 20, 50, "Response-2", Tuple.DOWN, AppEdge.MODULE);
        app.addAppEdge("WorkerModule-3", "MasterModule", 20, 50, "Response-3", Tuple.DOWN, AppEdge.MODULE);
        app.addAppEdge("MasterModule", "Actuators", 100, 50, "OutputData", Tuple.DOWN, AppEdge.ACTUATOR);

        app.addTupleMapping("MasterModule", " Sensor ", "Task-1", new FractionalSelectivity(0.3));
        app.addTupleMapping("MasterModule", " Sensor ", "Task-2", new FractionalSelectivity(0.3));
        app.addTupleMapping("MasterModule", " Sensor ", "Task-3", new FractionalSelectivity(0.3));
        app.addTupleMapping("WorkerModule-1", "Task-1", "Response-1", new FractionalSelectivity(1.0));
        app.addTupleMapping("WorkerModule-2", "Task-2", "Response-2", new FractionalSelectivity(1.0));
        app.addTupleMapping("WorkerModule-3", "Task-3", "Response-3", new FractionalSelectivity(1.0));
        app.addTupleMapping("MasterModule", "Response-1", "OutputData", new FractionalSelectivity(0.3));
        app.addTupleMapping("MasterModule", "Response-2", "OutputData", new FractionalSelectivity(0.3));
        app.addTupleMapping("MasterModule", "Response-3", "OutputData", new FractionalSelectivity(0.3));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("Sensor");
                add("MasterModule");
                add("WorkerModule-1");
                add("MasterModule");
                add("Actuator");
            }
        });
        final AppLoop loop2 = new AppLoop(new ArrayList<String>() {
            {
                add("Sensor");
                add("MasterModule");
                add("WorkerModule-2");
                add("MasterModule");
                add("Actuator");
            }
        });
        final AppLoop loop3 = new AppLoop(new ArrayList<String>() {
            {
                add("Sensor");
                add("MasterModule");
                add("WorkerModule-3");
                add("MasterModule");
                add("Actuator");
            }
        });
        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
                add(loop2);
                add(loop3);
            }
        };
        app.setLoops(loops);

        return app;
    }

    public Application createUnDirectedDataFlowApp(String appId, int brokerId) {

        Application application = Application.createApplication(appId, brokerId);

        application.addAppModule("Module1", 10);
        application.addAppModule("Module2", 10);
        application.addAppModule("Module3", 10);
        application.addAppModule("Module4", 10);
        application.addAppEdge("Sensor", "Module1", 3000, 500, "Sensor", Tuple.UP, AppEdge.SENSOR);

        application.addAppEdge("Module1", "Module2", 100, 1000, "ProcessedData-1", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Module2", "Module3", 100, 1000, "ProcessedData-2", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Module3", "Module4", 100, 1000, "ProcessedData-3", Tuple.UP, AppEdge.MODULE);

        application.addAppEdge("Module4", "Module1", 100, 1000, "ProcessedData-4", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("Module1", "Actuators", 100, 50, "OutputData", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("Module1", "Sensor", "ProcessedData-1", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module2", "ProcessedData-1", "ProcessedData-2", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module3", "ProcessedData-2", "ProcessedData-3", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module4", "ProcessedData-3", "ProcessedData-4", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module1", "ProcessedData-4", "OutputData", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("Sensor");
                add("Module1");
                add("Module2");
                add("Module3");
                add("Module4");
                add("Module1");
                add("Actuator");
            }
        });
        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };
        application.setLoops(loops);

        return application;
    }

    /**
     * Application Modules with different configuration The following Code
     * Snippet-4 creates modules with different configurations
     *
     */
    private static Application createApplication(String appId, int brokerId) {
        Application application = Application.createApplication(appId,
                brokerId);
        application.addAppModule("ClientModule", 20, 500, 1024, 1500);
        application.addAppModule("MainModule", 100, 1200, 4000, 100);

        application.addAppEdge("Sensor", "ClientModule", 3000, 500, "Sensor", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("ClientModule", "MainModule", 100, 1000, "PreProcessedData", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("MainModule", "ClientModule", 100, 1000, "ProcessedData", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("ClientModule", "Actuators", 100, 50, "OutputData", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("ClientModule", "Sensor", "PreProcessedData", new FractionalSelectivity(1.0));
        application.addTupleMapping("MainModule", "PreProcessedData", "ProcessedData", new FractionalSelectivity(1.0));
        application.addTupleMapping("ClientModule", "ProcessedData", "OutputData", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("Sensor");
                add("ClientModule");
                add("MainModule");
                add("Actuator");
            }
        });
        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };
        application.setLoops(loops);

        return application;
    }

    public Application createMasterWorker(String appId, int brokerId) {
        Application app = Application.createApplication(appId, brokerId);

        app.addAppModule("MasterModule", 10);
        app.addAppModule("WorkerModule", 10);

        app.addAppEdge("Sensor", "MasterModule", 3000, 500, "Sensor", Tuple.UP, AppEdge.SENSOR);
        app.addAppEdge("MasterModule", "WorkerModule", 100, 1000, "Task", Tuple.UP, AppEdge.MODULE);

        app.addAppEdge("WorkerModule", "MasterModule", 20, 50, "Response", Tuple.DOWN, AppEdge.MODULE);
        app.addAppEdge("MasterModule", "Actuator", 100, 50, "OutputData", Tuple.DOWN, AppEdge.ACTUATOR);

        app.addTupleMapping("MasterModule", " Sensor ", "Task", new FractionalSelectivity(0.3));
        app.addTupleMapping("WorkerModule", "Task", "Response", new FractionalSelectivity(1.0));
        app.addTupleMapping("MasterModule", "Response", "OutputData", new FractionalSelectivity(0.3));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("Sensor");
                add("MasterModule");
                add("WorkerModule");
                add("MasterModule");
                add("Actuator");
            }
        });
        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };
        app.setLoops(loops);

        return app;
    }

}
