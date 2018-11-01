package test;

import org.fog_impl.VideoFogBroker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import phd.ParseCmdLine;

/**
 * Simulation setup for case study 2 - Intelligent Surveillance
 *
 * @author Harshit Gupta
 *
 */
public class VideoFogMain {

    static Properties prop;

    static String propertiesFileURL;
    static String inputdataFolderURL;
    static String outputdataFileURL;

    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static Map<Integer, FogDevice> deviceById = new HashMap<Integer, FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();
    static List<Integer> idOfEndDevices = new ArrayList<Integer>();
    static Map<Integer, Map<String, Double>> deadlineInfo = new HashMap<Integer, Map<String, Double>>();
    static Map<Integer, Map<String, Integer>> additionalMipsInfo = new HashMap<Integer, Map<String, Integer>>();

    static boolean CLOUD = false;

    static int numOfGateways = 2;
    static int numOfEndDevPerGateway = 3;
    static double sensingInterval = 5;

    public static void main(String[] args) {

        Log.printLine("Starting Video Streame Service...");

        String[] arg = {"-property", "/home/futebol/simulation-tools/iFogSim/resources/config.properties",
            "-input", "/home/futebol/simulation-tools/iFogSim/resources/inputdataFile",
            "-output", "/home/futebol/simulation-tools/iFogSim/resources/outputFile/test.txt",
            "-stqprediction",
            "-videonum", "10",
            "-vmqueue", "1",
            "-vmNum", "0",
            "-appNum", "1"};

        prop = new Properties();

        ParseCmdLine pcl = new ParseCmdLine();
        CmdLineParser parser = new CmdLineParser(pcl);

        try {
            parser.parseArgument(arg);
            //pcl.run(test);
            propertiesFileURL = pcl.getPropertiesFileURL();
            System.out.println("**Property file url: " + propertiesFileURL);

            inputdataFolderURL = pcl.getInputdataFolderURL();
            System.out.println("**Input folder url: " + inputdataFolderURL);

            outputdataFileURL = pcl.getOutputdataFileURL();
            System.out.println("**Output file url: " + outputdataFileURL);

        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
        
        try {
            Log.disable();

            int num_user = 100; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "Video Stream"; // identifier of the application
            VideoFogBroker broker = new VideoFogBroker("broker");

            createFogDevices(broker.getId(), appId);

            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

//            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
//
//            for (int i = 0; i < idOfEndDevices.size(); i++) {
//                FogDevice fogDevice = deviceById.get(idOfEndDevices.get(i));
//                moduleMapping.addModuleToDevice("clientModule", fogDevice.getName());
//            }
//
//            if (true) {
//                moduleMapping.addModuleToDevice("storageModule", "cloud");
//            }

            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators, pcl);
            
            controller.setBroker(broker);
            
//            controller.submitApplication(application,
//                    (false) ? (new ModulePlacementMapping(fogDevices, application, moduleMapping))
//                            : (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            
            CloudSim.stopSimulation();

            Log.printLine("Video Streaming finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static double getvalue(double min, double max) {
        Random r = new Random();
        double randomValue = min + (max - min) * r.nextDouble();
        return randomValue;
    }

    private static int getvalue(int min, int max) {
        Random r = new Random();
        int randomValue = min + r.nextInt() % (max - min);
        return randomValue;
    }

    private static void createFogDevices(int userId, String appId) {
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);

        cloud.setParentId(-1);
        fogDevices.add(cloud);
        deviceById.put(cloud.getId(), cloud);

        for (int i = 0; i < numOfGateways; i++) {
            addGw(i + "", userId, appId, cloud.getId());
        }
    }

    private static void addGw(String gwPartialName, int userId, String appId, int parentId) {
        FogDevice gw = createFogDevice("g-" + gwPartialName, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
        fogDevices.add(gw);
        deviceById.put(gw.getId(), gw);
        gw.setParentId(parentId);
        gw.setUplinkLatency(10);

        for (int i = 0; i < numOfEndDevPerGateway; i++) {
            String endPartialName = gwPartialName + "-" + i;
            FogDevice end = addEnd(endPartialName, userId, appId, gw.getId());
            end.setUplinkLatency(2);

            fogDevices.add(end);
            deviceById.put(end.getId(), end);
        }
    }

    private static FogDevice addEnd(String endPartialName, int userId, String appId, int parentId) {

        FogDevice end = createFogDevice("e-" + endPartialName, 3200, 1000, 10000, 270, 2, 0, 87.53, 82.44);
        end.setParentId(parentId);
        idOfEndDevices.add(end.getId());

        Actuator actuator = new Actuator("a-" + endPartialName, userId, appId, "IoTActuator");
        actuators.add(actuator);

        actuator.setGatewayDeviceId(end.getId());
        actuator.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms

        return end;
    }

    @SuppressWarnings({"serial"})
    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("clientModule", 10, 1000, 1000, 100);
        application.addAppModule("storageModule", 10, 50, 12000, 100);

        application.addAppEdge("storageModule", "clientModule", 100, 0, 20000, "VideoStream", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("clientModule", "IoTActuator", 100, 0, 20000, "VideoStream", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("storageModule", "VideoStream", "VideoStream", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
            {
                add("storageModule");
                add("clientModule");
                add("IoTActuator");
            }
        });
        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop1);
            }
        };

        application.setLoops(loops);
        application.setDeadlineInfo(deadlineInfo);
        application.setAdditionalMipsInfo(additionalMipsInfo);

        return application;
    }

    private static FogDevice createFogDevice(String nodeName, long mips,
            int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<Pe>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );
        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        fogdevice.setMips((int) mips);

        return fogdevice;
    }

}
