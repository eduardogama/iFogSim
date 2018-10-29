/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog_impl.InstanceType;
import org.fog_impl.VideoSchedulerSpaceShared;
import org.fog_impl.VideoSegment;
import org.fog_impl.VideoStreams;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import phd.transcoder.TranscodingBroker;
import phd.transcoder.TranscodingDatacenter;
import phd.transcoder.TranscodingVm;

/**
 *
 * @author futebol
 */
public class Main {

    /**
     * The vmlist.
     */
    private static List<Vm> vmlist;
    private static int vmIdShift = 1;

    /**
     * Create datatcenter
     */
    private static TranscodingDatacenter datacenter;

    /**
     * Calculate the total cost
     */
    public static DatacenterCharacteristics characteristics;
    public static double memCost = 0;
    public static double storageCost = 0;
    public static double bwCost = 0;
    public static double vmCost = 0;
    public static double totalCost = 0;
    public static double totalTranscodingTime = 0;

    /*
     * URL file
     */
    public static String propertiesFileURL = null;
    public static String inputdataFolderURL = null;
    public static String outputdataFileURL = null;
    public static String gopDelayOutputURL = null;
    public static String sortalgorithm = null;
    public static String schedulingmethod = null;
    public static boolean startupqueue = true;
    public static int jobNum = 0;
    public static int waitinglist_max = 0;
    public static int vmNum = 0;
    public static String clusterType = null;
    public static String vmType = null;
    public static int frequency = 0;
    public static String estimatedLength = null;
    public static int seedShift = 0;
    public static double upthredshold = 0.0;
    public static double lowthredshold = 0.0;
    public static long rentingTime = 0;
    public static double testPeriod = 0.0;
    public static boolean stqprediction = true;
    public static boolean dropflag = true;

    //create period event
    private final static int PERIODIC_EVENT = 127;

    //All the instance share cloudletNewArrivalQueue and cloudletBatchqueue, both of them are synchronized list
    private static List<VideoSegment> cloudletNewArrivalQueue = Collections.synchronizedList(new ArrayList<VideoSegment>());
    private static List<VideoSegment> cloudletBatchQueue = Collections.synchronizedList(new ArrayList<VideoSegment>());
    public static Properties prop = new Properties();

    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static boolean CLOUD = true;

    static int numOfDepts = 4;
    static int numOfMobilesPerDept = 6;
    static double EEG_TRANSMISSION_TIME = 5.1;
    //static double EEG_TRANSMISSION_TIME = 10;

    public static void main(String[] args) {

        String[] arg = {"-property", "/home/futebol/simulation-tools/iFogSim/resources/config.properties",
            "-input", "/home/futebol/simulation-tools/iFogSim/resources/inputdataFile",
            "-output", "/home/futebol/simulation-tools/iFogSim/resources/outputFile",
            "-stqprediction",
            "-schedulingmethod", "FF",
            "-stqprediction",
            //"-videonum", "10",
            "-vmqueue", "1",
            "-vmNum", "0",
            "-clusterType", "homogeneous",
            "-vmType", "g2.2xlarge",
            "-vmfrequency", "10000",
            "-goplength", "AVERAGE",
            "-upthreshold", "0.05",
            "-lowthreshold", "0.01",
            "-testPeriod", "1200000",
            "-rentingTime", "20000",
            "-seedshift", "3"};

        ParseCmdLine pcl = new ParseCmdLine();
        CmdLineParser parser = new CmdLineParser(pcl);

        System.out.println(arg.length);

        try {
            parser.parseArgument(arg);

            //pcl.run(test);
            propertiesFileURL = pcl.getPropertiesFileURL();
            System.out.println("**Property file url: " + propertiesFileURL);

            inputdataFolderURL = pcl.getInputdataFolderURL();
            System.out.println("**Input folder url: " + inputdataFolderURL);

            outputdataFileURL = pcl.getOutputdataFileURL();
            System.out.println("**Output file url: " + outputdataFileURL);

            sortalgorithm = pcl.getSortAlgorithm();
            startupqueue = pcl.getStarupQueue();
            jobNum = pcl.getVideoNum();
            waitinglist_max = pcl.getVmQueueSize();
            vmNum = pcl.getVmNum();
            clusterType = pcl.getClusterType();
            vmType = pcl.getVmType();
            frequency = pcl.getVmFrequency();
            seedShift = pcl.getSeedShift();
            estimatedLength = pcl.getEstimatedGopLength();
            upthredshold = pcl.getUpThredshold();
            lowthredshold = pcl.getLowThreshold();
            rentingTime = pcl.getRentingTime();
            testPeriod = pcl.getTestPeriod();
            stqprediction = pcl.getStqPrediction();
            dropflag = pcl.getDropFlag();
            schedulingmethod = pcl.getSchedulingMethod();

        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }

        Log.printLine("Init Properties...");

        //set up configuration 
        OutputStream output = null;

        try {

            output = new FileOutputStream(propertiesFileURL);

            /**
             * Configuration properties for datacenter
             */
            prop.setProperty("datacenterMips", "100000");
            prop.setProperty("datacenterHostId", "0");
            prop.setProperty("datacenterRam", "163840"); //host memory 16GB
            prop.setProperty("datacenterStorage", "10485760"); //1TB
            prop.setProperty("datacenterBw", "163840"); //16GB

            /**
             * Configuration properties for VM
             */
            prop.setProperty("vmSize", "30720"); //30GB
            prop.setProperty("vmRam", "1024"); //vm memory(MB)
            prop.setProperty("vmMips", "8000");
            prop.setProperty("vmBw", "1024"); //1GB
            prop.setProperty("vmPesNumber", "1"); //number of cups
            prop.setProperty("vmName", "Xeon");

            if (vmNum == 0) {
                prop.setProperty("vmNum", "1");
                System.out.println("**The vm policy is: dynamic");
            } else {
                prop.setProperty("vmNum", String.valueOf(vmNum));
                System.out.println("**The number of Vm is: " + vmNum);
            }

            if (clusterType == null) {
                prop.setProperty("clusterType", "homogeneous");
            } else {
                prop.setProperty("clusterType", clusterType);
            }

            if (vmType == null) {
                prop.setProperty("vmType", "g2.2xlarge");
            } else {
                prop.setProperty("vmType", vmType);
            }

            //Configure VM renting time
            if (rentingTime == 0) {
                prop.setProperty("rentingTime", "60000");
                System.out.println("**VM renting time is: 60000ms");

            } else {
                prop.setProperty("rentingTime", String.valueOf(rentingTime));
                System.out.println("**VM renting time is: " + rentingTime + "ms");
            }

            /**
             * Configuration properties for cost
             */
            prop.setProperty("vmCostPerSec", "0.0000036");
            prop.setProperty("storageCostPerGb", "0.03");

            /**
             * Configuration of GOP
             */
            if (estimatedLength == null) {
                prop.setProperty("estimatedGopLength", "WORST");
            } else {
                prop.setProperty("estimatedGopLength", estimatedLength);
            }

            /**
             * Configuration properties for broker
             */
            if (sortalgorithm == null) {
                prop.setProperty("sortalgorithm", "SDF");
                System.out.println("**The sorting algorithm is: SDF...");
            } else {
                prop.setProperty("sortalgorithm", sortalgorithm);
                System.out.println("**The sorting algorithm is: " + sortalgorithm + "...");
            }

            if (schedulingmethod == null) {
                prop.setProperty("schedulingmethod", "MMUT");
                System.out.println("**The scheduling method is: MMUT...");
            } else {
                prop.setProperty("schedulingmethod", schedulingmethod);
                System.out.println("**The scheduling method is: " + schedulingmethod + "...");
            }

            if (startupqueue == true) {
                prop.setProperty("startupqueue", "true");
                System.out.println("**The process has startup queue....");
            } else {
                prop.setProperty("startupqueue", String.valueOf(startupqueue));
                System.out.println("**The process doesn't have startup queue...");
            }

            if (stqprediction == true) {
                prop.setProperty("stqprediction", "true");
                System.out.println("**The process has startup queue prediction....");
            } else {
                prop.setProperty("stqprediction", "false");
                System.out.println("**The process doesn't have startup queue prediction...");
            }

            if (dropflag == true) {
                prop.setProperty("dropflag", "true");
                System.out
                        .println("**The process turned on drop flag....");
            } else {
                prop.setProperty("dropflag", "false");
                System.out
                        .println("**The process turn off drop flag...");
            }

            prop.setProperty("seedShift", String.valueOf(seedShift));

            /**
             * configuration properties in Coordinator
             */
            File folder = new File(inputdataFolderURL);
            File[] listOfFiles = folder.listFiles();

            int jobCount = 0;

            for (File inputfile : listOfFiles) {
                if (inputfile.isFile() && inputfile.getName().toLowerCase().endsWith(".txt")) {
                    jobCount++;
                }
            }

            if (jobNum != 0) {
                prop.setProperty("periodEventNum", String.valueOf(jobNum));
                System.out.println("**There are " + jobNum + " videos...");
            } else {
                prop.setProperty("periodEventNum", String.valueOf(jobCount));
                System.out.println("**There are " + jobCount + " videos...");
            }

            //check vm provision frequence
            if (frequency == 0) {
                prop.setProperty("periodicDelay", "1000");
                System.out.println("**Vm checking frequency is: 1000ms");

            } else {
                prop.setProperty("periodicDelay", String.valueOf(frequency));
                System.out.println("**Vm checking frequency is: " + frequency);
            }

            //configure test time period
            if (testPeriod == 0.0) {
                prop.setProperty("testPeriod", "1200000");
                System.out.println("**Test Period is: 1200000");
            } else {
                prop.setProperty("testPeriod", String.valueOf(testPeriod));
                System.out.println("**Test Period is: " + testPeriod + "ms");
            }

            //The maximum number of vm a user can create
            prop.setProperty("MAX_VM_NUM", "100");

            /**
             * configuration properties in broker and datacenter's VM local
             * queue size
             */
            if (waitinglist_max == 0) {
                prop.setProperty("waitinglist_max", "2");
                System.out.println("**Vm local waiting queue size is: 2");
            } else {
                prop.setProperty("waitinglist_max", String.valueOf(waitinglist_max));
                System.out.println("**Vm local waiting queue size is: " + waitinglist_max);
            }

            /**
             * configuration properties for transcoding provisioning
             */
            if (upthredshold == 0) {
                prop.setProperty("DEADLINE_MISS_RATE_UPTH", "0.1");
                System.out.println("**deadline miss rate upthredshold is: 0.1");
            } else {
                prop.setProperty("DEADLINE_MISS_RATE_UPTH", String.valueOf(upthredshold));
                System.out.println("**deadline miss rate upthredshold is: " + upthredshold);

            }

            if (lowthredshold == 0) {
                prop.setProperty("DEADLINE_MISS_RATE_LOWTH", "0.05");
                System.out.println("**deadline miss rate lowthredshold is: 0.05");
            } else {
                prop.setProperty("DEADLINE_MISS_RATE_LOWTH", String.valueOf(lowthredshold));
                System.out.println("**deadline miss rate lowthredshold is: " + lowthredshold);
            }
            prop.setProperty("ARRIVAL_RATE_TH", "0.5");

            System.out.println('\n');

            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        Log.printLine("Starting ...");

        try {
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events
            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            //Second step: Create Datacenters
            //Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
            @SuppressWarnings("unused")
            TranscodingDatacenter datacenter0 = createDatacenter("Datacenter_0");

            //choose datacenter0 as the datacenter to create new vm, which can improve later
            datacenter = datacenter0;

            @SuppressWarnings("unused")
            //	TranscodingDatacenter datacenter1 = createDatacenter("Datacenter_1");

            //Third step: Create Broker and VideoStreams
            Coordinator coordinator = new Main().new Coordinator("Coordinator");
            //TranscodingMain.Coordinator coordinator = new Coordinator("Coordinator");	

            //Fourth Step: Create a initial VM list
//            vmlist = createVM(coordinator.getBroker().getId(), 2); //creating vms
//            coordinator.getBroker().submitVmList(vmlist);
            double startTranscodingTime = System.currentTimeMillis();

            //Fifth step: Starts the simulation
            CloudSim.startSimulation();

            totalTranscodingTime = (System.currentTimeMillis() - startTranscodingTime) / 1000;

            //Final step: Print results when simulation is over
            List<VideoSegment> newList = coordinator.getBroker().getCloudletSubmittedList();

            Map<Integer, Double> videoStartupTimeMap = coordinator.getBroker().getVideoStartupTimeMap();

            vmCost = coordinator.getBroker().getVmCost();
            storageCost = getStorageCost(newList);

            calculateTotalCost(storageCost, vmCost);

            printVideoStatistic(videoStartupTimeMap, newList);

            printCloudletList(newList);

            CloudSim.stopSimulation();

            String appId = "video_stream"; // identifier of the application

            FogBroker broker = new FogBroker("broker");

            Application app = createApplication(appId, broker.getId());

            app.setUserId(broker.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Application createApplication(String appId, int userId) {

        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        /*
         * Adding app modules 
         */
        application.addAppModule("client", 10); // adding module Client to the application model
        application.addAppModule("stream", 10); // adding module Stream to the application model

        /*
         * Adding app edges
         */
        application.addAppEdge("client", "stream", 3500, 3500, "REQUEST", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("stream", "client", 3500, 3500, "RESPONSE", Tuple.UP, AppEdge.MODULE);

        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules. 
         */
        application.addTupleMapping("client", "GLOBAL_STREAM_VIDEO", "GLOBAL_STREAM_UPDATE", new FractionalSelectivity(0.9));

        final AppLoop loop = new AppLoop(new ArrayList<String>() {
            {
                add("DISPLAY");
            }
        });

        List<AppLoop> loops = new ArrayList<AppLoop>() {
            {
                add(loop);
            }
        };

        application.setLoops(loops);

        return application;
    }

    private static TranscodingDatacenter createDatacenter(String name) {

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

        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
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

        /*DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);*/
        characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        TranscodingDatacenter datacenter = null;
        try {
            datacenter = new TranscodingDatacenter(name, propertiesFileURL, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static List<TranscodingVm> createVM(int userId, String vmType, int vms, int idShift, long rentingTime) {

        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<TranscodingVm> list = new LinkedList<TranscodingVm>();

        InstanceType it = new InstanceType(vmType);

        long size = it.getInstanceStorageSize();
        int ram = it.getInstanceRam();
        int mips = it.getInstanceMips();
        long bw = it.getInstanceBw();
        int pesNumber = it.getInstancePesNumber();
        double costPerSec = it.getInstanceCost();
        String vmm = "Xeon";
        double periodicUtilizationRate = 0.0;

        TranscodingVm[] vm = new TranscodingVm[vms];

        for (int i = 0; i < vms; i++) {
            //always run vm#0
            if (idShift == 0) {
                vm[i] = new TranscodingVm(i + idShift, userId, mips, pesNumber, ram, bw, size, Long.MAX_VALUE, costPerSec, periodicUtilizationRate, vmType, vmm, new VideoSchedulerSpaceShared());
                System.out.println("A " + vmType + " EC2 instance has been created....");
            } else {
                vm[i] = new TranscodingVm(i + idShift, userId, mips, pesNumber, ram, bw, size, rentingTime, costPerSec, periodicUtilizationRate, vmType, vmm, new VideoSchedulerSpaceShared());
                System.out.println("A " + vmType + " EC2 instance has been created....");
            }
            list.add(vm[i]);
        }

        return list;
    }

    //Generate random number for random length cloudlets
    private static long getRandomNumber(int aStart, int aEnd, Random aRandom) {
        if (aStart > aEnd) {
            throw new IllegalArgumentException("Start cannot exceed End.");
        }
        //get the range, casting to long to avoid overflow problems
        long range = (long) aEnd - (long) aStart + 1;
        // compute a fraction of the range, 0 <= frac < range
        long fraction = (long) (range * aRandom.nextDouble());
        long randomNumber = (long) (fraction + aStart);

        return randomNumber;
    }

    private static double getBwCost(List<VideoSegment> list) {
        for (VideoSegment cl : list) {
            bwCost += cl.getProcessingCost();
        }
        return bwCost;
    }

    private static double getStorageCost(List<VideoSegment> list) {
        double byteConvertToGb = 1024 * 1024 * 1024;
        double storageSize = 0;
        for (VideoSegment cl : list) {
            storageSize += cl.getCloudletFileSize();
            storageSize += cl.getCloudletOutputSize();
        }
        System.out.println("The storage size is: " + new DecimalFormat("#0.00").format(storageSize));
        storageCost = storageSize / byteConvertToGb * characteristics.getCostPerStorage();

        return storageCost;
    }

    private static void calculateTotalCost(double storageCost, double vmCost) {
        totalCost = vmCost;
        System.out.println("\n");
        System.out.println("*******The storage cost is: " + new DecimalFormat("#0.0000").format(storageCost));
        System.out.println("*******The time cost is: " + new DecimalFormat("#0.0000").format(vmCost));
        System.out.println("*******The total cost is: " + new DecimalFormat("#0.0000").format(totalCost));
    }

    /**
     * Print out each video's start up time and average startup time
     *
     * @param videoStartupTimeMap
     * @throws IOException
     */
    private static void printVideoStatistic(Map<Integer, Double> videoStartupTimeMap, List<VideoSegment> list) throws IOException {
        System.out.println("\n");

        double videoStartupTimeAverage = 0;
        int videoCount = 0;
        int size = list.size();
        int deadlineMeetCount = 0;
        double totalUtilityGain = 0;
        double utilityRate = 0.0;

        for (VideoSegment cl : list) {
            if (cl.getCloudletDeadline() > cl.getFinishTime()) {
                totalUtilityGain += cl.getUtilityNum();
                deadlineMeetCount++;
            }
        }

        utilityRate = totalUtilityGain / deadlineMeetCount;

        VideoSegment cloudlet;
        Map<Integer, Integer> videoGopNumMap = new HashMap<Integer, Integer>();
        Map<Integer, Integer> videoDeadlineMissNumMap = new HashMap<Integer, Integer>();
        Map<Integer, Double> videoDeadlineMissRateMap = new HashMap<Integer, Double>();
        Map<Integer, Double> videoBufferDelayMap = new HashMap<Integer, Double>();
        Map<Integer, Integer> gopOrderNumMap = new HashMap<Integer, Integer>();

        int deadlineMissCount = 0;

        for (VideoSegment cl : list) {
            if (!videoDeadlineMissNumMap.containsKey(cl.getCloudletVideoId())) {

                videoGopNumMap.put(cl.getCloudletVideoId(), 1);
                videoDeadlineMissNumMap.put(cl.getCloudletVideoId(), 0);
                if (cl.getCloudletDeadline() < cl.getFinishTime()) {
                    videoDeadlineMissNumMap.put(cl.getCloudletVideoId(), videoDeadlineMissNumMap.get(cl.getCloudletVideoId()) + 1);
                }
            } else {
                videoGopNumMap.put(cl.getCloudletVideoId(), videoGopNumMap.get(cl.getCloudletVideoId()) + 1);
                if (cl.getCloudletDeadline() < cl.getFinishTime()) {
                    videoDeadlineMissNumMap.put(cl.getCloudletVideoId(), videoDeadlineMissNumMap.get(cl.getCloudletVideoId()) + 1);
                }
            }
        }

        for (Map.Entry<Integer, Integer> entry : videoGopNumMap.entrySet()) {
            double videoDeadlineMissNum = videoDeadlineMissNumMap.get(entry.getKey());
            double videoGopNum = videoGopNumMap.get(entry.getKey());
            double videoDeadlineMissRate = videoDeadlineMissNum / videoGopNum;
            videoDeadlineMissRateMap.put(entry.getKey(), videoDeadlineMissRate);
        }

        for (VideoSegment vsg : list) {
            if (!videoBufferDelayMap.containsKey(vsg.getCloudletId())) {

                videoBufferDelayMap.put(vsg.getCloudletId(), vsg.getFinishTime() - vsg.getArrivalTime());
                gopOrderNumMap.put(vsg.getCloudletId(), 1);

            } else {
                videoBufferDelayMap.put(vsg.getCloudletId(), (videoBufferDelayMap.get(vsg.getCloudletId()) + vsg.getFinishTime() - vsg.getArrivalTime()));
                gopOrderNumMap.put(vsg.getCloudletId(), (gopOrderNumMap.get(vsg.getCloudletId()) + 1));
            }
        }

        PrintWriter pw2 = new PrintWriter(new FileWriter(gopDelayOutputURL, true));

        for (Integer key : videoBufferDelayMap.keySet()) {
            if (key == 0) {
                pw2.printf("%-18s%-18s%-20s", "Scheduling", "GOP#", "StartupTime");
                pw2.println("\n");
                pw2.printf("%-18s%-18d%-20.2f", prop.getProperty("schedulingmethod", "MM"), key, videoBufferDelayMap.get(key) / gopOrderNumMap.get(key));
                pw2.println("\n");
            } else {
                pw2.printf("%-18s%-18d%-20.2f", prop.getProperty("schedulingmethod", "MM"), key, videoBufferDelayMap.get(key) / gopOrderNumMap.get(key));
                pw2.println("\n");
            }
        }
        pw2.close();

        PrintWriter pw = new PrintWriter(new FileWriter(outputdataFileURL, true));

        for (Integer vId : videoStartupTimeMap.keySet()) {
            videoStartupTimeAverage += videoStartupTimeMap.get(vId);
            videoCount++;
        }

        //pw.close();
        double totalAverageStartupTime = videoStartupTimeAverage / videoCount;
        System.out.println("Video average start up time is: " + new DecimalFormat("#0.0000").format(totalAverageStartupTime));

        double deadlineMissNum = 0;
        double totalDeadlineMissRate;

        for (VideoSegment cl : list) {
            if (cl.getCloudletDeadline() < cl.getFinishTime()) {
                deadlineMissNum++;
            }
        }

        totalDeadlineMissRate = deadlineMissNum / list.size();

        if (startupqueue) {
            /**
             * ouput with new arrival queuue
             */
            if (vmNum == 0) {
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25s%-20s%-12s%-12s%-12s%-12s", "Startup Queue", "Scheduling", "Video Numbers", "VM Number ", "VQS", "Average Startup Time", "Deadline Miss Rate", "Total Cost", "UtilityGain", "DMR_UPTH", "DMR_LTH");
                pw.println("\n");
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25.2f%-20.4f%-12.4f%-12.4f%-12.4f%-12.4f", "YES", prop.getProperty("schedulingmethod", "MM"), prop.getProperty("periodEventNum", "2"), "Dynamic", prop.getProperty("waitinglist_max", "2"), totalAverageStartupTime, totalDeadlineMissRate, totalCost, utilityRate, Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_UPTH", "0.10")), Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_LOWTH", "0.05")));

                pw.println("\n");
            } else {
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25s%-20s%-12s%-12s%-12s%-12s", "Startup Queue", "Scheduling", "Video Numbers", "VM Number ", "VQS", "Average Startup Time", "Deadline Miss Rate", "Total Cost", "UtilityGain", "DMR_UPTH", "DMR_LTH");
                pw.println("\n");
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25.2f%-20.4f%-12.4f%-12.4f%-12.4f%-12.4f", "YES", prop.getProperty("schedulingmethod", "MM"), prop.getProperty("periodEventNum", "2"), prop.getProperty("vmNum", "2"), prop.getProperty("waitinglist_max", "2"), totalAverageStartupTime, totalDeadlineMissRate, totalCost, utilityRate, Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_UPTH", "0.10")), Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_LOWTH", "0.05")));

                pw.println("\n");
            }
        } else /**
         * ouput without new arrival queuue
         */
        {
            if (vmNum == 0) {
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25s%-20s%-12s%-12s%-12s%-12s", "Startup Queue", "Scheduling", "Video Numbers", "VM Number ", "VQS", "Average Startup Time", "Deadline Miss Rate", "Total Cost", "UtilityGain", "DMR_UPTH", "DMR_LTH");
                pw.println("\n");
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25.2f%-20.4f%-12.4f%-12.4f%-12.4f%-12.4f", "NO", prop.getProperty("schedulingmethod", "MM"), prop.getProperty("periodEventNum", "2"), "Dynamic", prop.getProperty("waitinglist_max", "2"), totalAverageStartupTime, totalDeadlineMissRate, totalCost, utilityRate, Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_UPTH", "0.10")), Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_LOWTH", "0.05")));

                pw.println("\n");
            } else {
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25s%-20s%-12s%-12s%-12s%-12s", "Startup Queue", "Sorting", "Video Numbers", "VM Number ", "VQS", "Average Startup Time", "Deadline Miss Rate", "Total Cost", "UtilityGain", "DMR_UPTH", "DMR_LTH");
                pw.println("\n");
                pw.printf("%-18s%-10s%-20s%-16s%-10s%-25.2f%-20.4f%-12.4f%-12.4f%-12.4f%-12.4f", "NO", prop.getProperty("schedulingmethod", "MM"), prop.getProperty("periodEventNum", "2"), prop.getProperty("vmNum", "2"), prop.getProperty("waitinglist_max", "2"), totalAverageStartupTime, totalDeadlineMissRate, totalCost, utilityRate, Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_UPTH", "0.10")), Double.valueOf(prop.getProperty("DEADLINE_MISS_RATE_LOWTH", "0.05")));

                pw.println("\n");

            }
        }

        pw.close();
    }

    /**
     * Prints the Cloudlet objects
     *
     * @param list list of Cloudlets
     * @throws Exception
     */
    private static void printCloudletList(List<VideoSegment> list) throws Exception {
        int size = list.size();
        VideoSegment cloudlet;
        int deadlineMissCount = 0;
        double totalDeadlineMissRate;

        int deadlineMeetCount = 0;
        double totalUtilityGain = 0;
        double utilityRate = 0.0;

        for (VideoSegment cl : list) {
            if (cl.getCloudletDeadline() < cl.getFinishTime()) {
                deadlineMissCount++;
            } else {
                totalUtilityGain += cl.getUtilityNum();
                deadlineMeetCount++;
            }
        }

        utilityRate = totalUtilityGain / deadlineMeetCount;

        /*System.out.println("\nThere are: " + list.size() + " cloudlets...");
		System.out.println("\nThere are: " + deadlineMissCount + " cloudlets missed deadline...");*/
        totalDeadlineMissRate = (double) deadlineMissCount / list.size();

        System.out.println("\nThe total deadline miss rate is: " + new DecimalFormat("#0.0000").format(totalDeadlineMissRate));
        System.out.println("\nThe total utilty gain is: " + new DecimalFormat("#0.0000").format(totalUtilityGain));
        System.out.println("\nThe utilty rate is: " + new DecimalFormat("#0.0000").format(utilityRate));

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        System.out.format("%-18s%-18s%-18s%-18s%-18s%-18s%-18s%-18s%-18s%-18s", "Video ID", "Cloudlet ID",
                "STATUS", "Data center ID", "VM ID", "Arrival Time", "Start Exec Time", "Exec Time", "Finish Time", "Deadline");
        System.out.println("\n");

        //DecimalFormat dft = new DecimalFormat("###.##");
        DecimalFormat dft = new DecimalFormat("###");
        for (int i = 0; i < size; i++) {
            cloudlet = (VideoSegment) list.get(i);
            Log.print(indent + cloudlet.getCloudletVideoId() + indent + indent + indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == VideoSegment.SUCCESS) {
                Log.print("SUCCESS");

                System.out.format("%-18d%-18d%-18s%-18d%-18d%-18.2f%-18.2f%-18.2f%-18.2f%-18.2f", cloudlet.getCloudletVideoId(), cloudlet.getCloudletId(), "SUCCESS",
                        cloudlet.getResourceId(), cloudlet.getVmId(), cloudlet.getArrivalTime(), cloudlet.getExecStartTime(), cloudlet.getActualCPUTime(),
                        cloudlet.getFinishTime(), cloudlet.getCloudletDeadline());

            }
        }
        System.out.println("There are " + size + " cloudlets are processed.");
    }

    public class Coordinator extends SimEntity {

        private static final int CREATE_BROKER = 150;

        private static final int PERIODIC_UPDATE = 153;

        private static final int CREATE_JOB = 155;

        private static final int DROP_VIDEO = 128;

        private static final int CHECK_UTILIZATION = 130;

        private static final int CREATE_VM_TYPE = 133;

        private int MAX_VM_NUM;

        public int videoId = 0;

        private int periodCount = 0;

        //Max jobs we are about to create
        //private final static int periodEventNum = 50;
        private int periodEventNum;

        // private final static int periodicDelay = 20000; //contains the delay to the next periodic event
        private int periodicDelay;
        //   public boolean generatePeriodicEvent = true; //true if new internal events have to be generated

        private List<TranscodingVm> vmList;
        private List<VideoSegment> cloudletList;
        private TranscodingBroker broker;

        private int jobCount = 0;
        private int previousJobCount = 0;
        private double jobDelay = 0.0;
        private int vmNumber = 0;
        private String clusterType = null;
        private String staticVmType = null;
        private int seedShift = 0;
        private String estimatedGopLength = null;
        private double DEADLINE_MISS_RATE_UPTH = 0.0;
        private double DEADLINE_MISS_RATE_LOWTH = 0.0;
        private double testPeriod = 0.0;
        private long rentingTime = 0;
        private double utilizationCheckPeriod = 1000;

        private boolean prediction = true;
        private boolean stqprediction = true;
        private boolean startupqueue = true;
        private boolean dropflag = true;

        Map<TranscodingVm, Double> cpuUtilizationMap = new HashMap<TranscodingVm, Double>();
        int cupUtilizationPeriodCount = 0;

        //private Properties prop = new Properties();
        public Coordinator(String name) throws IOException {
            super(name);
            Properties prop = new Properties();

            //InputStream input = new FileInputStream("/Users/lxb200709/Documents/TransCloud/cloudsim/modules/cloudsim-impl/config.properties");
            InputStream input = new FileInputStream(propertiesFileURL);
            prop.load(input);

            String jobNum;
            String frequence;
            String MAX_VM_NUM;
            String vmNumber;
            String seedShift;
            String upTh;
            String lowTh;
            String testPeriod;
            String rentingTime;
            String stqprediction;
            String startupqueue;
            String dropflag;
            String staticVmType;
            String clusterType;

            jobNum = prop.getProperty("periodEventNum");
            this.periodEventNum = Integer.valueOf(jobNum);

            frequence = prop.getProperty("periodicDelay");
            this.periodicDelay = Integer.valueOf(frequence);

            MAX_VM_NUM = prop.getProperty("MAX_VM_NUM", "16");
            this.MAX_VM_NUM = Integer.valueOf(MAX_VM_NUM);

            vmNumber = prop.getProperty("vmNum");
            this.vmNumber = Integer.valueOf(vmNumber);

            clusterType = prop.getProperty("clusterType");
            this.clusterType = clusterType;

            staticVmType = prop.getProperty("vmType");
            this.staticVmType = staticVmType;

            seedShift = prop.getProperty("seedShift");
            this.seedShift = Integer.valueOf(seedShift);

            upTh = prop.getProperty("DEADLINE_MISS_RATE_UPTH", "0.2");
            this.DEADLINE_MISS_RATE_UPTH = Double.valueOf(upTh);

            lowTh = prop.getProperty("DEADLINE_MISS_RATE_LOWTH", "0.01");
            this.DEADLINE_MISS_RATE_LOWTH = Double.valueOf(lowTh);

            testPeriod = prop.getProperty("testPeriod", "1800000");
            this.testPeriod = Double.valueOf(testPeriod);

            rentingTime = prop.getProperty("rentingTime", "60000");
            this.rentingTime = Long.valueOf(rentingTime);

            stqprediction = prop.getProperty("stqprediction", "true");
            this.stqprediction = Boolean.valueOf(stqprediction);

            startupqueue = prop.getProperty("startupqueue", "true");
            this.startupqueue = Boolean.valueOf(startupqueue);

            dropflag = prop.getProperty("dropflag", "true");
            this.dropflag = Boolean.valueOf(dropflag);

            estimatedGopLength = prop.getProperty("estimatedGopLength");
        }

        @Override
        /**
         * Open video stream arrival rate file and scheduling the initial tasks
         * of creates jobs and VMs
         */
        public void startEntity() {
            Log.printLine(getName() + " is starting...");
            schedule(getId(), 0, CREATE_BROKER);
            schedule(getId(), 1, CREATE_JOB);

            if (dropflag) {
                schedule(getId(), 2, DROP_VIDEO);
            }

            send(getId(), utilizationCheckPeriod, CHECK_UTILIZATION);
            send(getId(), periodicDelay, PERIODIC_UPDATE);
        }

        @Override
        public void processEvent(SimEvent ev) {
            switch (ev.getTag()) {
                case CREATE_BROKER:
                    setBroker(createBroker());
                    //create and submit intial vm list
                    System.out.println(">>>>>>>>> " + vmNum);
                    if (vmNum == 0) {
                        setVmList(createVM(broker.getId(), "g2.2xlarge", 1, 0, Long.MAX_VALUE));
                        broker.submitVmList(getVmList());
                    } else if (!startupqueue) {
                        if (clusterType.equals("homogeneous")) {
                            for (int i = 0; i < vmNum; i++) {
                                setVmList(createVM(broker.getId(), staticVmType, 1, i, Long.MAX_VALUE));
                                broker.submitVmList(getVmList());
                            }
                        } else {
                            for (int i = 0; i < 4; i++) {
                                setVmList(createVM(broker.getId(), "g2.2xlarge", 1, i, Long.MAX_VALUE));
                                broker.submitVmList(getVmList());
                            }
                            for (int i = 4; i < 8; i++) {
                                setVmList(createVM(broker.getId(), "c4.xlarge", 1, i, Long.MAX_VALUE));
                                broker.submitVmList(getVmList());
                            }
                            for (int i = 8; i < 10; i++) {
                                setVmList(createVM(broker.getId(), "r3.xlarge", 1, i, Long.MAX_VALUE));
                                broker.submitVmList(getVmList());
                            }
                            for (int i = 10; i < 12; i++) {
                                setVmList(createVM(broker.getId(), "m4.large", 1, i, Long.MAX_VALUE));
                                broker.submitVmList(getVmList());
                            }
                        }
                    } else {
                        setVmList(createVM(broker.getId(), "c4.xlarge", vmNum, 0, Long.MAX_VALUE));
                        broker.submitVmList(getVmList());
                    }
                    //broker.submitVmList(getVmList());
                    break;

                case CREATE_JOB:
                    processNewJobs();
                    break;
                case PERIODIC_UPDATE:
                    processProvisioning();
                    break;

                // drop videos
                case DROP_VIDEO:
                    dropVideo();
                    break;

                case CHECK_UTILIZATION:
                    checkCpuUtilization();
                    break;

                default:
                    Log.printLine(getName() + ": unknown event type");
                    break;
            }
        }

        @Override
        public void shutdownEntity() {
            Log.printLine(getName() + " is shuting down...");
        }

        /**
         * PS: Check this method later. Understand event process first. Allocate
         * VMs based on Startup Queue Length
         */
        public void provisionVM(long vmNum, String vmType) {
//        if (vmType == null) {
//            vmType = "c4.xlarge";
//        }
//        //	System.out.println("\n**creating "+ vmNum + " " + vmType + " vms...\n");
//        for (int i = 0; i < vmNum; i++) {
//            List<TranscodingVm> vmNew = (List<TranscodingVm>) createVM(broker.getId(), vmType, 1, vmIdShift, rentingTime);
//            vmIdShift++;
//            //submit it to broker
//            broker.submitVmList(vmNew);
//            //creat a event for datacenter to create a vm
//            sendNow(datacenter.getId(), CloudSimTags.VM_CREATE_ACK, vmNew.get(0));
//        }
        }

        public List<TranscodingVm> getVmList() {
            return vmList;
        }

        protected void setVmList(List<TranscodingVm> vmList) {
            this.vmList = vmList;
        }

        public List<VideoSegment> getCloudletList() {
            return cloudletList;
        }

        protected void setCloudletList(List<VideoSegment> cloudletList) {
            this.cloudletList = cloudletList;
        }

        public TranscodingBroker getBroker() {
            return broker;
        }

        public void setBroker(TranscodingBroker broker) {
            this.broker = broker;
        }

        public int getJobCount() {
            return jobCount;
        }

        public double getJobDelay() {
            return jobDelay;
        }

        /*
         * We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
         * to the specific rules of the simulated scenario
         */
        private TranscodingBroker createBroker() {
            TranscodingBroker broker = null;
            try {
                broker = new TranscodingBroker("TranscodingBroker", characteristics, propertiesFileURL);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return broker;
        }

        public List<TranscodingVm> createVM(int userId, String vmType, int vms, int idShift, long rentingTime) {

            //Creates a container to store VMs. This list is passed to the broker later
            LinkedList<TranscodingVm> list = new LinkedList<TranscodingVm>();

            InstanceType it = new InstanceType(vmType);

            long size = it.getInstanceStorageSize();
            int ram = it.getInstanceRam();
            int mips = it.getInstanceMips();
            long bw = it.getInstanceBw();
            int pesNumber = it.getInstancePesNumber();
            double costPerSec = it.getInstanceCost();
            String vmm = "Xeon";
            double periodicUtilizationRate = 0.0;

            TranscodingVm[] vm = new TranscodingVm[vms];

            for (int i = 0; i < vms; i++) {
                //always run vm#0
                if (idShift == 0) {
                    vm[i] = new TranscodingVm(i + idShift, userId, mips, pesNumber, ram, bw, size, Long.MAX_VALUE, costPerSec, periodicUtilizationRate, vmType, vmm, new VideoSchedulerSpaceShared());
                    System.out.println("A " + vmType + " EC2 instance has been created....");
                } else {
                    vm[i] = new TranscodingVm(i + idShift, userId, mips, pesNumber, ram, bw, size, rentingTime, costPerSec, periodicUtilizationRate, vmType, vmm, new VideoSchedulerSpaceShared());
                    System.out.println("A " + vmType + " EC2 instance has been created....");
                }
                list.add(vm[i]);
            }

            return list;
        }

        /**
         * Periodically process new jobs
         */
        private void processNewJobs() {
            System.out.println(CloudSim.clock() + " : Creating a new job....");

            int brokerId = getBroker().getId();

            // Create a thread pool that can create video streams
            Random random = new Random();

            //List<Cloudlet> newList = new ArrayList<Cloudlet>();
            ExecutorService executorService = Executors.newFixedThreadPool(2);

            CompletionService<String> taskCompletionService = new ExecutorCompletionService<String>(executorService);

            try {
                ArrayList<Callable<String>> callables = new ArrayList<Callable<String>>();
                for (int i = 0; i < 1; i++) {
                    int cloudlets = (int) getRandomNumber(10, 50, random);
                    callables.add(new VideoStreams(videoId + "", inputdataFolderURL, startupqueue, estimatedGopLength, seedShift, brokerId, videoId, cloudlets));
                    // Thread.sleep(1000);
                    videoId++;
                }

                // List<Callable<String>> callables = createCallableList();
                for (Callable<String> callable : callables) {
                    taskCompletionService.submit(callable);
                    // Thread.sleep(500);
                }

                // Thread.sleep(1000);
                for (int i = 0; i < callables.size(); i++) {
                    Future<String> result = taskCompletionService.take();
                    System.out.println(result.get() + " End.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace(); // no real error handling. Don't do this in production!
            } catch (ExecutionException e) {
                e.printStackTrace(); // no real error handling. Don't do this in production!
            }

            executorService.shutdown();

            VideoStreams vt = new VideoStreams();
            cloudletBatchQueue = vt.getBatchQueue();
            cloudletNewArrivalQueue = vt.getNewArrivalQueue();

            //update the cloudlet list and back to simulation
            broker.submitCloudletList(cloudletBatchQueue, cloudletNewArrivalQueue);
            
            if (vmNum == 0 && stqprediction) {
                List<Integer> videoIdList = new ArrayList<Integer>();
                int newVideoNum = 0;
                int vmToBeCreated = 0;
                double val = 0.0;
                for (VideoSegment vs : broker.getHighestPriorityCloudletList()) {
                    if (vs.getCloudletId() == 0) {
                        newVideoNum++;
                    }
                }

                val = (newVideoNum - 1) / (DEADLINE_MISS_RATE_UPTH * 10);
                if (val > 0) {
                    vmToBeCreated = (int) val;

                    System.out.println("\n******There are " + newVideoNum + " new videos are in the priority queue, creating " + vmToBeCreated + "c4.xlarge...\n");
                    provisionVM(vmToBeCreated, null);
                }
            }

            broker.submitCloudlets();

            //Check if there are more jobs to process
            if (broker.isGeneratePeriodicEvent()) {

                jobCount++;
                if (jobCount < periodEventNum) {
                    Random r = new Random(jobCount);

                    //  jobDelay = (int)getRandomNumber(3000, 3000, random);	
                    /*jobDelay = 180000.00/periodEventNum;
			    	send(getId(),jobDelay,CREATE_JOB);*/
                    jobDelay = testPeriod / periodEventNum;

                    double val = r.nextGaussian() * (jobDelay / 3);
                    double stdJobDelay = val + jobDelay;

                    send(getId(), stdJobDelay, CREATE_JOB);

                } else {
                    broker.setGeneratePeriodicEvent(false);
                }
            }
            //periodCount++;

            CloudSim.resumeSimulation();
        }

        private void processProvisioning() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void dropVideo() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void checkCpuUtilization() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
}
