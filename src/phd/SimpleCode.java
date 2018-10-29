/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static javax.xml.bind.JAXBIntrospector.getValue;
import static jdk.xml.internal.JdkXmlUtils.getValue;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import phd.apps.ApplicationModel;

/**
 *
 * @author futebol
 */
public class SimpleCode {

    final static boolean CLOUD = true;

    static int numOfFogDevices = 100;
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static Map<String, Integer> getIdByName = new HashMap<String, Integer>();

    static Map<Integer, Pair<Double, Integer>> mobilityMap = new HashMap<Integer, Pair<Double, Integer>>();
    static String mobilityDestination = "FogDevice-0";

    public static void main(String[] args) {

        try {
            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "Simple Code"; // identifier of the application

            FogBroker broker = new FogBroker("broker");

            Application app = new ApplicationModel().createMasterWorker(appId, broker.getId());
            app.setUserId(broker.getId());

            createFogDevices(broker.getId(), appId);

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping

            for (FogDevice device : fogDevices) {
                if (device.getName().startsWith("F")) { // names of all devices start with 'F' 
                    System.out.println("Name Device -> " + device.getName());
                    moduleMapping.addModuleToDevice("MasterModule", device.getName());  // fixing 1 instance of the Motion Detector module to each Smart Camera
                }
            }

            moduleMapping.addModuleToDevice("WorkerModule", "cloud"); // fixing instances of User Interface module in the Cloud
            moduleMapping.addModuleToDevice("Actuator", "cloud"); // fixing instances of User Interface module in the Cloud
            
            ModulePlacement mPlacement = (CLOUD) ? (new ModulePlacementMapping(fogDevices, app, moduleMapping))
                    : (new ModulePlacementEdgewards(fogDevices, sensors, actuators, app, moduleMapping));
            
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
            controller.setMobilityMap(mobilityMap);

            controller.submitApplication(app, mPlacement);

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            Log.printLine("SimpleCode finished!");

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void createFogDevices(int userId, String appId) {

        FogDevice cloud = createAFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
        cloud.setParentId(-1);
        fogDevices.add(cloud);
        getIdByName.put(cloud.getName(), cloud.getId());

        for (int i = 0; i < numOfFogDevices; i++) {
            FogDevice device = addDevice(i + "", userId, appId, cloud.getId());

            device.setParentId(cloud.getId());
            device.setUplinkLatency(10);

            fogDevices.add(device);
            getIdByName.put(device.getName(), device.getId());
        }
    }

    private static FogDevice addDevice(String id, int userId, String appId, int parentId) {

        FogDevice device = createAFogDevice("FogDevice-" + id, getValue(12000, 15000), getValue(4000, 8000),
                getValue(200, 300), getValue(500, 1000), 1, 0.01, getValue(100, 120), getValue(70, 75));

        device.setParentId(parentId);

        Sensor sensor = new Sensor("s-" + id, "Sensor", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of camera (sensor) follows a deterministic distribution
        sensors.add(sensor);

        Actuator ptz = new Actuator("act-" + id, userId, appId, "OutputData");
        actuators.add(ptz);

        sensor.setGatewayDeviceId(device.getId());
        sensor.setLatency(1.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms

        ptz.setGatewayDeviceId(device.getId());
        ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms

        return device;
    }

    private static FogDevice createAFogDevice(String nodeName, long mips, int ram,
            long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {

        List<Pe> peList = new ArrayList<Pe>();
        List<Host> hostList = new ArrayList<Host>();
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw),
                storage, peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));

        hostList.add(host);

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch,
                os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        FogDevice fogdevice = null;

        try {
            fogdevice = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fogdevice.setLevel(level);

        return fogdevice;
    }

    private static FogDevice addLowLevelFogDevice(String id, int brokerId, String appId, int parentId) {

        FogDevice lowLevelFogDevice = createAFogDevice("LowLevelFogDevice-" + id, 1000, 1000, 10000, 270, 2, 0, 87.53, 82.44);
        lowLevelFogDevice.setParentId(parentId);

        getIdByName.put(lowLevelFogDevice.getName(), lowLevelFogDevice.getId());

        Sensor sensor = new Sensor("s-" + id, "Sensor", brokerId, appId, new DeterministicDistribution(getVal(5.00)));

        sensors.add(sensor);
        Actuator actuator = new Actuator("a-" + id, brokerId, appId, "OutputData");

        actuators.add(actuator);

        sensor.setGatewayDeviceId(lowLevelFogDevice.getId());
        sensor.setLatency(6.0);
        actuator.setGatewayDeviceId(lowLevelFogDevice.getId());
        actuator.setLatency(1.0);

        return lowLevelFogDevice;
    }

    private static double getVal(double min) {
        Random rn = new Random();
        return rn.nextDouble() * 10 + min;
    }

    private static FogDevice addLowLevelFogDevice(String id, int brokerId, String appId) {

        FogDevice lowLevelFogDevice = createAFogDevice("LowLevelFogDevice-" + id, 1000, 1000, 10000, 270, 2, 0, 87.53, 82.44);
        lowLevelFogDevice.setParentId(-1);
        lowLevelFogDevice.setxCoordinate(getVal(10.00));
        lowLevelFogDevice.setyCoordinate(getVal(15.00));

        getIdByName.put(lowLevelFogDevice.getName(), lowLevelFogDevice.getId());

        Sensor sensor = new Sensor("s-" + id, "Sensor", brokerId, appId, new DeterministicDistribution(getVal(5.00)));
        sensors.add(sensor);

        sensor.setGatewayDeviceId(lowLevelFogDevice.getId());
        sensor.setLatency(6.0);

        Actuator actuator = new Actuator("a-" + id, brokerId, appId, "OutputData");
        actuators.add(actuator);

        actuator.setGatewayDeviceId(lowLevelFogDevice.getId());
        actuator.setLatency(1.0);

        return lowLevelFogDevice;
    }

    private static FogDevice AddLowLevelFogDevice(String id, int brokerId, String appId, int parentId) {

        FogDevice lowLevelFogDevice = createAFogDevice("LowLevelFog-Device -" + id, 1000, 1000, 10000, 270, 2, 0, 87.53, 82.44);
        lowLevelFogDevice.setParentId(parentId);

        getIdByName.put(lowLevelFogDevice.getName(), lowLevelFogDevice.getId());

        if ((int) (Math.random() * 100) % 2 == 0) {
            Pair<Double, Integer> pair = new Pair<Double, Integer>(100.00, getIdByName.get(mobilityDestination));
            mobilityMap.put(lowLevelFogDevice.getId(), pair);
        }

        Sensor sensor = new Sensor("s-" + id, "Sensor", brokerId, appId, new DeterministicDistribution(getVal(5.00)));
        sensors.add(sensor);

        sensor.setGatewayDeviceId(lowLevelFogDevice.getId());
        sensor.setLatency(6.0);

        Actuator actuator = new Actuator("a-" + id, brokerId, appId, "OutputData");
        actuators.add(actuator);

        actuator.setGatewayDeviceId(lowLevelFogDevice.getId());
        actuator.setLatency(1.0);

        return lowLevelFogDevice;
    }

}
