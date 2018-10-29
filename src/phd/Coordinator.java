/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog_impl.InstanceType;
import org.fog_impl.VideoSchedulerSpaceShared;
import org.fog_impl.VideoSegment;
import org.fog_impl.VideoStreams;
import static phd.Main.inputdataFolderURL;
import static phd.Main.vmNum;
import phd.transcoder.TranscodingBroker;
import phd.transcoder.TranscodingVm;

/**
 * Coordinator class is used to control the whole cloud system: 1. Reading New
 * Video streams 2. Split Video Streams 3. Create Broker 4. Based VMProvision
 * allocate and deallocate VMs.
 *
 * @author Bill
 *
 */
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

    private DatacenterCharacteristics characteristics;
    private String propertiesFileURL;

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
    public Coordinator(String name, String propertiesFileURL, DatacenterCharacteristics characteristics) throws IOException {
        super(name);
        Properties prop = new Properties();

        this.characteristics = characteristics;
        this.propertiesFileURL = propertiesFileURL;

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
     * Open video stream arrival rate file and scheduling the initial tasks of
     * creates jobs and VMs
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
     * PS: Check this method later. Understand event process first. Allocate VMs
     * based on Startup Queue Length
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
     * Generate random number for random length cloudlets
     */
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
                callables.add(new VideoStreams("" + videoId, inputdataFolderURL, startupqueue, estimatedGopLength, seedShift, brokerId, videoId, cloudlets));
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
            // no real error handling. Don't do this in production!
            e.printStackTrace();
        } catch (ExecutionException e) {
            // no real error handling. Don't do this in production!
            e.printStackTrace();
        }
        
        executorService.shutdown();
        
        
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
