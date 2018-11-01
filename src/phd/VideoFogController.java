/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import static org.cloudbus.cloudsim.sdn.example.SDNBroker.appId;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog_impl.VideoFogBroker;
import org.fog_impl.VideoSegment;
import org.fog_impl.VideoStreams;
import phd.apps.ApplicationModel;

/**
 *
 * @author futebol
 */
public class VideoFogController extends SimEntity {

    private static final int CREATE_JOB = 150;
    private static final int PERIODIC_UPDATE = 153;
    private static final int CHECK_UTILIZATION = 130;

    public static boolean ONLY_CLOUD = false;

    private static List<VideoSegment> cloudletNewArrivalQueue = Collections.synchronizedList(new ArrayList<VideoSegment>());
    private static List<VideoSegment> cloudletBatchQueue = Collections.synchronizedList(new ArrayList<VideoSegment>());
    private ParseCmdLine properties;

    private int periodicDelay;
    private double utilizationCheckPeriod = 1000;

    private List<FogDevice> fogDevices;
    private List<Sensor> sensors;
    private List<Actuator> actuators;

    private Map<String, Application> applications;
    private Map<String, Integer> appLaunchDelays;

    private Map<String, ModulePlacement> appModulePlacementPolicy;

    private static Map<Integer, Pair<Double, Integer>> mobilityMap;
    static Map<Integer, Integer> clusterInfo = new HashMap<Integer, Integer>();
    static Map<Integer, List<Integer>> clusters = new HashMap<Integer, List<Integer>>();
    private VideoFogBroker broker;
    private int videoId = 0;

    public VideoFogController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
        super(name);
        this.applications = new HashMap<String, Application>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
        for (FogDevice fogDevice : fogDevices) {
            fogDevice.setControllerId(getId());
        }
        setFogDevices(fogDevices);
        setActuators(actuators);
        setSensors(sensors);
        connectWithLatencies();

        gatewaySelection();
//        formClusters();
    }

    public VideoFogController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, ParseCmdLine properties) {
        super(name);
        this.applications = new HashMap<String, Application>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());

        for (FogDevice fogDevice : fogDevices) {
            fogDevice.setControllerId(getId());
        }

        setFogDevices(fogDevices);
        setActuators(actuators);
        setSensors(sensors);
        setProperties(properties);
        connectWithLatencies();

        gatewaySelection();
//        formClusters();
    }

    private FogDevice getFogDeviceById(int id) {
        for (FogDevice fogDevice : getFogDevices()) {
            if (id == fogDevice.getId()) {
                return fogDevice;
            }
        }
        return null;
    }

    private void connectWithLatencies() {
        for (FogDevice fogDevice : getFogDevices()) {
            FogDevice parent = getFogDeviceById(fogDevice.getParentId());
            if (parent == null) {
                continue;
            }
            double latency = fogDevice.getUplinkLatency();
            parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
            parent.getChildrenIds().add(fogDevice.getId());
        }
    }

    @Override
    public void startEntity() {
        schedule(getId(), 0, CREATE_JOB);

        send(getId(), utilizationCheckPeriod, CHECK_UTILIZATION);
        send(getId(), periodicDelay, PERIODIC_UPDATE);
    }

    private void StartSim() {
        for (String appId : applications.keySet()) {
            if (getAppLaunchDelays().get(appId) == 0) {
                processAppSubmit(applications.get(appId));
            } else {
                send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
            }
        }

        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
        send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);

        for (FogDevice dev : getFogDevices()) {
            sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);
        }

        scheduleMobility();
    }

    public void setMobilityMap(Map<Integer, Pair<Double, Integer>> mobilityMap) {
        this.mobilityMap = mobilityMap;
    }

    private void scheduleMobility() {
        if (mobilityMap != null) {
            for (int id : mobilityMap.keySet()) {
                Pair<Double, Integer> pair = mobilityMap.get(id);

                double mobilityTime = pair.getFirst();

                int mobilityDestinationId = pair.getSecond();

                Pair<Integer, Integer> newConnection = new Pair<Integer, Integer>(id, mobilityDestinationId);

                send(getId(), mobilityTime, FogEvents.FUTURE_MOBILITY, newConnection);
            }
        }
    }

    /*
     *   Users can add other required instructions on manageMobility method to deal with the mobility 
     *   driven issues such as AppModule migration and connection with latency
     */
    private void manageMobility(SimEvent ev) {
        Pair<Integer, Integer> pair = (Pair<Integer, Integer>) ev.getData();

        int deviceId = pair.getFirst();
        int newParentId = pair.getSecond();

        FogDevice deviceWithMobility = getFogDeviceById(deviceId);
        FogDevice mobilityDest = getFogDeviceById(newParentId);

        deviceWithMobility.setParentId(newParentId);

        System.out.println(CloudSim.clock() + " " + deviceWithMobility.getName() + " is now connected to " + mobilityDest.getName());
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.APP_SUBMIT:
                processAppSubmit(ev);
                break;
            case FogEvents.TUPLE_FINISHED:
                processTupleFinished(ev);
                break;
            case FogEvents.CONTROLLER_RESOURCE_MANAGE:
                manageResources();
                break;
            case FogEvents.STOP_SIMULATION:
                CloudSim.stopSimulation();
                printTimeDetails();
                printPowerDetails();
                printCostDetails();
                printNetworkUsageDetails();
                System.exit(0);
                break;
            case FogEvents.FUTURE_MOBILITY:
                manageMobility(ev);
                break;

            case CREATE_JOB:
                processNewJobs();
                break;

            case PERIODIC_UPDATE:
                processProvisioning();
                break;
            case CHECK_UTILIZATION:
                checkCpuUtilization();
                break;
        }
    }

    private void printNetworkUsageDetails() {
        System.out.println("Total network usage = " + NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME);
    }

    private FogDevice getCloud() {
        for (FogDevice dev : getFogDevices()) {
            if (dev.getName().equals("cloud")) {
                return dev;
            }
        }
        return null;
    }

    private void printCostDetails() {
        System.out.println("Cost of execution in cloud = " + getCloud().getTotalCost());
    }

    private void printPowerDetails() {
        for (FogDevice fogDevice : getFogDevices()) {
            System.out.println(fogDevice.getName() + " : Energy Consumed = " + fogDevice.getEnergyConsumption());
        }
    }

    private String getStringForLoopId(int loopId) {
        for (String appId : getApplications().keySet()) {
            Application app = getApplications().get(appId);
            for (AppLoop loop : app.getLoops()) {
                if (loop.getLoopId() == loopId) {
                    return loop.getModules().toString();
                }
            }
        }
        return null;
    }

    private void printTimeDetails() {
        System.out.println("=========================================");
        System.out.println("============== RESULTS ==================");
        System.out.println("=========================================");
        System.out.println("EXECUTION TIME : " + (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
        System.out.println("=========================================");
        System.out.println("APPLICATION LOOP DELAYS");
        System.out.println("=========================================");
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
            /*double average = 0, count = 0;
             for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
             Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
             Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
             if(startTime == null || endTime == null)
             break;
             average += endTime-startTime;
             count += 1;
             }
             System.out.println(getStringForLoopId(loopId) + " ---> "+(average/count));*/
            System.out.println(getStringForLoopId(loopId) + " ---> " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
        }
        System.out.println("=========================================");
        System.out.println("TUPLE CPU EXECUTION DELAY");
        System.out.println("=========================================");

        for (String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
            System.out.println(tupleType + " ---> " + TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
        }

        System.out.println("=========================================");
    }

    protected void manageResources() {
        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
    }

    private void processTupleFinished(SimEvent ev) {
    }

    @Override
    public void shutdownEntity() {
    }

    public void submitApplication(Application application, int delay, ModulePlacement modulePlacement) {
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        getAppLaunchDelays().put(application.getAppId(), delay);
        getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);

        for (Sensor sensor : sensors) {
            sensor.setApp(getApplications().get(sensor.getAppId()));
        }
        for (Actuator ac : actuators) {
            ac.setApp(getApplications().get(ac.getAppId()));
        }

        for (AppEdge edge : application.getEdges()) {
            if (edge.getEdgeType() == AppEdge.ACTUATOR) {
                String moduleName = edge.getSource();
                for (Actuator actuator : getActuators()) {
                    if (actuator.getActuatorType().equalsIgnoreCase(edge.getDestination())) {
                        application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
                    }
                }
            }
        }
    }

    public void submitApplication(Application application, ModulePlacement modulePlacement) {
        submitApplication(application, 0, modulePlacement);
    }

    private void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        processAppSubmit(app);
    }

    private void processAppSubmit(Application application) {
        System.out.println(CloudSim.clock() + " Submitted application " + application.getAppId());
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);

        ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
        for (FogDevice fogDevice : fogDevices) {
            sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }

        Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
        for (Integer deviceId : deviceToModuleMap.keySet()) {
            for (AppModule module : deviceToModuleMap.get(deviceId)) {
                sendNow(deviceId, FogEvents.APP_SUBMIT, application);
                sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
            }
        }
    }

    private void gatewaySelection() {
        // TODO Auto-generated method stub
        for (int i = 0; i < getFogDevices().size(); i++) {
            FogDevice fogDevice = getFogDevices().get(i);
            int parentID = -1;
            if (fogDevice.getParentId() == -1) {
                double minDistance = Config.MAX_NUMBER;
                for (int j = 0; j < getFogDevices().size(); j++) {
                    FogDevice anUpperDevice = getFogDevices().get(j);
                    if (fogDevice.getLevel() + 1 == anUpperDevice.getLevel()) {
                        double distance = calculateDistance(fogDevice, anUpperDevice);
                        if (distance < minDistance) {
                            minDistance = distance;
                            parentID = anUpperDevice.getId();
                        }
                    }
                }
            }
            fogDevice.setParentId(parentID);
        }
    }

    private double calculateDistance(FogDevice fogDevice, FogDevice anUpperDevice) {
        // TODO Auto-generated method stub
        return Math.sqrt(Math.pow(fogDevice.getxCoordinate() - anUpperDevice.getxCoordinate(), 2.00)
                + Math.pow(fogDevice.getyCoordinate() - anUpperDevice.getyCoordinate(), 2.00));
    }

    private void formClusters() {
        for (FogDevice fd : getFogDevices()) {
            clusterInfo.put(fd.getId(), -1);
        }
        int clusterId = 0;
        for (int i = 0; i < getFogDevices().size(); i++) {
            FogDevice fd1 = getFogDevices().get(i);
            for (int j = 0; j < getFogDevices().size(); j++) {
                FogDevice fd2 = getFogDevices().get(j);
                if (fd1.getId() != fd2.getId() && fd1.getParentId() == fd2.getParentId()
                        && calculateDistance(fd1, fd2) < Config.CLUSTER_DISTANCE && fd1.getLevel() == fd2.getLevel()) {
                    int fd1ClusteriD = clusterInfo.get(fd1.getId());
                    int fd2ClusteriD = clusterInfo.get(fd2.getId());
                    if (fd1ClusteriD == -1 && fd2ClusteriD == -1) {
                        clusterId++;
                        clusterInfo.put(fd1.getId(), clusterId);
                        clusterInfo.put(fd2.getId(), clusterId);
                    } else if (fd1ClusteriD == -1) {
                        clusterInfo.put(fd1.getId(), clusterInfo.get(fd2.getId()));
                    } else if (fd2ClusteriD == -1) {
                        clusterInfo.put(fd2.getId(), clusterInfo.get(fd1.getId()));
                    }
                }
            }
        }
        for (int id : clusterInfo.keySet()) {
            if (!clusters.containsKey(clusterInfo.get(id))) {
                List<Integer> clusterMembers = new ArrayList<Integer>();
                clusterMembers.add(id);
                clusters.put(clusterInfo.get(id), clusterMembers);
            } else {
                List<Integer> clusterMembers = clusters.get(clusterInfo.get(id));
                clusterMembers.add(id);
                clusters.put(clusterInfo.get(id), clusterMembers);
            }
        }
        for (int id : clusters.keySet()) {
            System.out.println(id + " " + clusters.get(id));
        }
    }

    public ParseCmdLine getProperties() {
        return properties;
    }

    public void setProperties(ParseCmdLine properties) {
        this.properties = properties;
    }

    public List<FogDevice> getFogDevices() {
        return fogDevices;
    }

    public void setFogDevices(List<FogDevice> fogDevices) {
        this.fogDevices = fogDevices;
    }

    public Map<String, Integer> getAppLaunchDelays() {
        return appLaunchDelays;
    }

    public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
        this.appLaunchDelays = appLaunchDelays;
    }

    public Map<String, Application> getApplications() {
        return applications;
    }

    public void setApplications(Map<String, Application> applications) {
        this.applications = applications;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        for (Sensor sensor : sensors) {
            sensor.setControllerId(getId());
        }
        this.sensors = sensors;
    }

    public List<Actuator> getActuators() {
        return actuators;
    }

    public void setActuators(List<Actuator> actuators) {
        this.actuators = actuators;
    }

    public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
        return appModulePlacementPolicy;
    }

    public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
        this.appModulePlacementPolicy = appModulePlacementPolicy;
    }

    public VideoFogBroker getBroker() {
        return this.broker;
    }

    public void setBroker(VideoFogBroker broker) {
        this.broker = broker;
    }

    private void processNewJobs() {
        System.out.println(CloudSim.clock() + " : Creating a new job....");

        int brokerId = getBroker().getId();

        // Create a thread pool that can create video streams
        Random random = new Random();

        //List<Cloudlet> newList = new ArrayList<Cloudlet>();
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletionService<String> taskCompletionService = new ExecutorCompletionService<String>(
                executorService);
        try {

            ArrayList<Callable<String>> callables = new ArrayList<Callable<String>>();
            for (int i = 0; i < 1; i++) {
                int cloudlets = (int) getRandomNumber(10, 50, random);
                callables.add(new VideoStreams(videoId + "", properties.getInputdataFolderURL(), brokerId, videoId, properties));
                videoId++;
            }

            for (Callable<String> callable : callables) {
                taskCompletionService.submit(callable);
            }

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

        broker.submitCloudletList(cloudletBatchQueue, cloudletNewArrivalQueue);

        Application application = ApplicationModel.createVideoApplication(appId + "", broker.getId());
        application.setUserId(broker.getId());
        //Next step: implement application modules

        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

        for (FogDevice fogDevice : getFogDevices()) {
            if (fogDevice.getName().startsWith("e")) {
                moduleMapping.addModuleToDevice("clientModule", fogDevice.getName());
            }
        }

        if (true) {
            moduleMapping.addModuleToDevice("storageModule", "cloud");
        }
        
        submitApplication(application,new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping));
        StartSim();
    }

    public void processProvisioning() {

    }

    public void checkCpuUtilization() {

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

}
