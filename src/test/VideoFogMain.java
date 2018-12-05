package test;

import org.fog_impl.VideoFogBroker;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import phd.MapInstance;
import phd.ParseCmdLine;
import phd.VideoFogController;
import phd.apps.ApplicationModel;
import phd.utils.MaxAndMin;

/**
 * globo Simulation setup for case study 2 - Intelligent Surveillance
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

    static List<FogDevice> ap_devices = new ArrayList<>();
    static List<VideoFogBroker> brokerList = new ArrayList<VideoFogBroker>();

    static List<String> applIdList = new ArrayList<String>();
    static List<Application> applications = new ArrayList<Application>();

    static Map<String, FogDevice> nodes = new HashMap<String, FogDevice>();

    static List<MapInstance> mapUsers = new ArrayList<MapInstance>();

    private static Random rand;
    private static int seed;

    public static void main(String[] args) {

        Log.printLine("Starting Video Streame Service...");

        String[] arg = {"-property", "/home/eduardo/simulation-tools/iFogSim/resources/config.properties",
            "-input", "/home/futebol/simulation-tools/iFogSim/resources/inputdataFile",
            "-output", "/home/futebol/simulation-tools/iFogSim/resources/outputFile/test.txt",
            "-stqprediction",
            "-videonum", "10",
            "-vmqueue", "1",
            "-vmNum", "0",
            "-appNum", "1",
            "-cloudNum", "1",
            "-apNum", "1",
            "-gwNum", "1",
            "-euNum", "5"};

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
            Log.enable();

            int num_user = 100; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            setRand(new Random(getSeed() * Integer.MAX_VALUE));
            setSeed(30); //It is used to generate randomize aspects. Use 30 seeds.
            if (getSeed() < 1) {
                System.out.println("Seed cannot be less than 1");
                System.exit(0);
            }

            System.out.print("Creating Cloud ... ");
            FogDevice cloud = createCloud("cloud", -1); // Creating AP Devices
            nodes.put("cloud", cloud);
            System.out.println("Cloud created.");

            System.out.print("Creating access Points ... ");
            for (int i = 0; i < pcl.getApNum(); i++) {
                FogDevice node = createAPDevices(i + ""); // Creating AP Devices   

                node.setParentId(cloud.getId());
                node.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms

                nodes.put("ap-" + i, node);
                System.out.print("ap-" + i + " created ... ");
            }
            System.out.println("Access Points created.");

            System.out.print("Creating Gateways ... ");
            for (int i = 0; i < pcl.getGwNum(); i++) {
                FogDevice node = createGWDevices(i + ""); // Creating AP Devices   

                node.setParentId(nodes.get("ap-" + i).getId());
                node.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms

                nodes.put("gw-" + i, node);

                System.out.print("Creating End-users Devices ... ");
                for (int j = 0; j < pcl.getEuNum(); j++) {
                    FogDevice eu = createEUDevices(j + ""); // Creating AP Devices

                    eu.setParentId(node.getId());
                    eu.setUplinkLatency(2);

                    nodes.put("eu-" + i + j, eu);
                    mapUsers.add(new MapInstance("eu-" + i + "-" + j, "", null, null, eu, null));
                }
                System.out.print("End-users Devices created ... ");
            }
            System.out.println("Gateways created.");

            System.out.println("Creating brokers ... ");
            String appId = "Video Stream"; // identifier of the application

            AtomicInteger k = new AtomicInteger(0);
            mapUsers.forEach((user) -> {
                VideoFogBroker broker = null;
                try {
                    broker = new VideoFogBroker("broker-" + user.getName());
                } catch (Exception ex) {
                    Logger.getLogger(VideoFogMain.class.getName()).log(Level.SEVERE, null, ex);
                }

                System.out.print("Linking Actuator a-" + user.getName() + " for " + broker.getName() + " ... ");
                Actuator display = new Actuator("a-" + user.getName(), broker.getId(), "app-" + k.getAndIncrement(), "DISPLAY");

                display.setGatewayDeviceId(user.getDevice().getId());
                display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms

                brokerList.add(broker);
                actuators.add(display);

                user.setBroker(broker);
                user.setActuator(display);
                System.out.println("Actuator a-" + user.getName() + " for " + broker.getName() + " created.");

            });
            System.out.println("Brokers created.");

//            System.out.print("Creating a VM with content movies in Cloud ...");
//            CloudletScheduler cloudletScheduler = new TupleScheduler(500, 1);
//            long sizeVm = (MaxAndMin.MIN_VM_SIZE + (long) ((MaxAndMin.MAX_VM_SIZE - MaxAndMin.MIN_VM_SIZE)
//                    * (getRand().nextDouble())));
//
//            AppModule vmMovies = new AppModule(cloud.getId() // id
//                    , "AppModuleVm_" + cloud.getName() //name
//                    , "MyApp_vr_game" + cloud.getId() //appId
//                    , nodes.get("cloud").getId() //userId
//                    , 2000 //mips
//                    , 64 //(int) sizeVm/3 
//                    , 1000 //bw
//                    , sizeVm, "Vm_" + cloud.getName() //vmm
//                    , cloudletScheduler, new HashMap<Pair<String, String>, SelectivityModel>());
//
//            cloud.setVmMovies(vmMovies);
//            System.out.println("VM in cloud created.");
//
//            
//            System.out.print("Creating VMs in end-users devices ...");
//            mapUsers.forEach((user) -> {
//                CloudletScheduler cloudletSchedulerEU = new TupleScheduler(500, 1);
//                long sizeVmEU = (MaxAndMin.MIN_VM_SIZE + (long) ((MaxAndMin.MAX_VM_SIZE - MaxAndMin.MIN_VM_SIZE)
//                        * (getRand().nextDouble())));
//
//                AppModule vmMoviesEU = new AppModule(user.getDevice().getId() // id
//                        , "app-module-vm-" + user.getDevice().getName() //name
//                        , "video-stream-" + user.getDevice().getId() //appId
//                        , user.getDevice().getId() //userId
//                        , 2000 //mips
//                        , 64 //(int) sizeVm/3 
//                        , 1000 //bw
//                        , sizeVmEU, "Vm_" + user.getDevice().getName() //vmm
//                        , cloudletScheduler, new HashMap<Pair<String, String>, SelectivityModel>());
//
//                user.getDevice().setVmMovies(vmMoviesEU);
//            });
//            
//            //Each broker receive a VM
//            mapUsers.forEach((user) -> {
//                List<Vm> tmpVmList = new ArrayList<Vm>();
//                tmpVmList.add(user.getDevice().getLocalVm());
//                user.getBroker().submitVmList(tmpVmList);
//            });
//            System.out.println("VMs end-users devices created.");

            System.out.print("Creating Applications in end-users devices ...");
            AtomicInteger i = new AtomicInteger(0);
            mapUsers.forEach(user -> {
                String appName = "app-" + i.getAndIncrement();

                Application app = ApplicationModel.createVideoApplication(appName, user.getBroker().getId());

//                applIdList.add(appName);
                user.setApp(app);
            });
            System.out.println("Applications created.");

            System.out.print("Creating Mapping, Controller and Application ...");
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping

            mapUsers.forEach((user) -> {
                user.getApp().setPlacementStrategy("Mapping");
            });

            mapUsers.forEach((user) -> {
                moduleMapping.addModuleToDevice("client-" + user.getAppId(), user.getDevice().getName());

                System.out.println("client-" + user.getApp().getAppId());
            });

            if (true) {
                moduleMapping.addModuleToDevice("storageModule", "cloud");
            }
            List<FogDevice> list_nodes = nodes.values().stream().collect(Collectors.toList());

            VideoFogController controller = new VideoFogController("master-controller", list_nodes, brokerList, sensors, actuators, pcl);

            controller.setUsers(mapUsers);

            mapUsers.forEach((user) -> {
                controller.submitApplication(user.getApp(),
                        (new ModulePlacementEdgewards(list_nodes, sensors, actuators, user.getApp(), moduleMapping)));
            });
//            controller.setBroker(video_broker);
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

    private static FogDevice createCloud(String userId, int id) {
        // creates the fog device Cloud at the apex of the hierarchy with level=0
//        FogDevice cloud = createFogDevice(userId, //NodeName
//                44800, //mips
//                40000, //ram
//                100, //upBw
//                10000, //downBw
//                0, //level
//                0.01, //rateMips
//                16 * 103, //busyPower
//                16 * 83.25); //idlePower
//        cloud.setParentId(-1);
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.setParentId(-1);

        return cloud;
    }

    private static FogDevice createAPDevices(String id) {
        return createFogDevice("ap-" + id, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
    }

    private static FogDevice createGWDevices(String id) {
        return createFogDevice("gw-" + id, 1000, 1000, 10000, 270, 2, 0, 87.53, 82.44);
    }

    private static FogDevice createEUDevices(String id) {
        return createFogDevice("eu-" + id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
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
                add("DISPLAY");
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

    public static Random getRand() {
        return rand;
    }

    public static void setRand(Random rand) {
        VideoFogMain.rand = rand;
    }

    public static int getSeed() {
        return seed;
    }

    public static void setSeed(int seed) {
        VideoFogMain.seed = seed;
    }

    private static Datacenter createDatacenter(String name) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store one or more
        //    Machines
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
        //    create a list to store these PEs before creating
        //    a Machine.
        List<Pe> peList1 = new ArrayList<Pe>();

        //int mips = 8000;
        String mipsStr = prop.getProperty("datacenterMips", "8000");
        int mips = Integer.valueOf(mipsStr);

        // 3. Create PEs and add these into the list.
        //for a quad-core machine, a list of 4 PEs is required:
        peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
        peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(4, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
        peList1.add(new Pe(5, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(6, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(7, new PeProvisionerSimple(mips)));

        //Another list, for a dual-core machine
        List<Pe> peList2 = new ArrayList<Pe>();

        peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(3, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(4, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(5, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(6, new PeProvisionerSimple(mips)));
        peList2.add(new Pe(7, new PeProvisionerSimple(mips)));

        List<Pe> peList3 = new ArrayList<Pe>();

        peList3.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList3.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList3.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList3.add(new Pe(3, new PeProvisionerSimple(mips)));
        peList3.add(new Pe(4, new PeProvisionerSimple(mips)));
        peList3.add(new Pe(5, new PeProvisionerSimple(mips)));
        peList3.add(new Pe(6, new PeProvisionerSimple(mips)));
        peList3.add(new Pe(7, new PeProvisionerSimple(mips)));

        List<Pe> peList4 = new ArrayList<Pe>();

        peList4.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList4.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList4.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList4.add(new Pe(3, new PeProvisionerSimple(mips)));
        peList4.add(new Pe(4, new PeProvisionerSimple(mips)));
        peList4.add(new Pe(5, new PeProvisionerSimple(mips)));
        peList4.add(new Pe(6, new PeProvisionerSimple(mips)));
        peList4.add(new Pe(7, new PeProvisionerSimple(mips)));

        List<Pe> peList5 = new ArrayList<Pe>();

        peList5.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList5.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList5.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList5.add(new Pe(3, new PeProvisionerSimple(mips)));
        peList5.add(new Pe(4, new PeProvisionerSimple(mips)));
        peList5.add(new Pe(5, new PeProvisionerSimple(mips)));
        peList5.add(new Pe(6, new PeProvisionerSimple(mips)));
        peList5.add(new Pe(7, new PeProvisionerSimple(mips)));

        List<Pe> peList6 = new ArrayList<Pe>();

        peList6.add(new Pe(0, new PeProvisionerSimple(mips)));
        peList6.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList6.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList6.add(new Pe(3, new PeProvisionerSimple(mips)));
        peList6.add(new Pe(4, new PeProvisionerSimple(mips)));
        peList6.add(new Pe(5, new PeProvisionerSimple(mips)));
        peList6.add(new Pe(6, new PeProvisionerSimple(mips)));
        peList6.add(new Pe(7, new PeProvisionerSimple(mips)));

        //4. Create Hosts with its id and list of PEs and add them to the list of machines
        /*int hostId=0;
		int ram = 16384; //host memory (MB)
		long storage = 1000000; //host storage
		int bw = 10000;*/
        prop.setProperty("datacenterMips", "8000");
        prop.setProperty("datacenterHostId", "0");
        prop.setProperty("datacenterRam", "131072"); //host memory 128GB
        prop.setProperty("datacenterStorage", "1048576"); //1TB
        prop.setProperty("datacenterBw", "131072"); //128GB

        String storageStr = prop.getProperty("datacenterStorage", "1048576");
        String ramStr = prop.getProperty("datacenterRam", "131072");
        String bwStr = prop.getProperty("datacenterBw", "131072"); //16GB
        String hostIdStr = prop.getProperty("datacenterHostId", "0");

        long storage = Long.valueOf(storageStr);
        int ram = Integer.valueOf(ramStr);
        long bw = Long.valueOf(bwStr);
        int hostId = Integer.valueOf(hostIdStr);

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList1,
                        new VmSchedulerTimeShared(peList1)
                )
        ); // This is our first machine

        hostId++;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList2,
                        new VmSchedulerTimeShared(peList2)
                )
        ); // Second machine

        hostId++;
        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList3,
                        new VmSchedulerTimeShared(peList3)
                )
        );

        hostId++;
        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList4,
                        new VmSchedulerTimeShared(peList4)
                )
        );

        hostId++;
        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList5,
                        new VmSchedulerTimeShared(peList5)
                )
        );

        hostId++;
        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList6,
                        new VmSchedulerTimeShared(peList6)
                )
        );

        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xeon";

        String vmCostStr = prop.getProperty("vmCostPerSec", "0.0000036");
        String storageCostStr = prop.getProperty("storageCostPerGb", "0.03");

        double cost = Double.valueOf(vmCostStr);
        double costPerStorage = Double.valueOf(storageCostStr);

        double time_zone = 10.0;         // time zone this resource located
        //double cost = 0.013/3600;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        //double costPerStorage = 0.1;	// the cost of using storage in this resource
        double costPerBw = 0.1;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

}
