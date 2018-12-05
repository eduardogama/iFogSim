/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog_impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.fog.entities.FogBroker;
import org.fog_impl.VideoSegment;
import phd.transcoder.TranscodingVm;

/**
 *
 * @author futebol
 */
public class VideoFogBroker extends FogBroker {

    /**
     * The cloudlet new arrival list.
     */
    protected List<VideoSegment> cloudletNewList = new ArrayList<VideoSegment>();
    protected List<VideoSegment> cloudletList = new ArrayList<VideoSegment>();

    public static List<VideoSegment> highestPriorityCloudletList = new ArrayList<VideoSegment>();

    //create sending Application event
    public final static int CLOUDLET_SUBMIT_RESUME = 125;
    //exchange completion time between datacenter and broker	
    public final static int ESTIMATED_COMPLETION_TIME = 126;
    //create period event
    public final static int PERIODIC_EVENT = 127;

    //create period drop video event
    public final static int DROP_VIDEO = 128;

    //create a vm type event
    private static final int CREATE_VM_TYPE = 133;

    //video Id
    public static int videoId = 1;

    double periodicDelay = 5; //contains the delay to the next periodic event
    boolean generatePeriodicEvent = true; //true if new internal events have to be generated

    //All the instance share cloudletNewArrivalQueue and cloudletBatchqueue, both of them are synchronized list
    private static List<VideoSegment> cloudletNewArrivalQueue = Collections.synchronizedList(new ArrayList<VideoSegment>());
    private static List<VideoSegment> cloudletBatchQueue = Collections.synchronizedList(new ArrayList<VideoSegment>());

    public List<TranscodingVm> vmDestroyedList = new ArrayList<TranscodingVm>();

    //set size of local vm queue
    //private final static int waitinglist_max = 2;
    private int waitinglist_max;

    public Map<Integer, Double> totalCompletionTime_vmMap = new HashMap<Integer, Double>();
    public static Map<Integer, Double> totalCompletionTime_vmMap_Min = new HashMap<Integer, Double>();

    //Track the disply start up time
    private Map<Integer, Double> displayStartupTimeMap = new HashMap<Integer, Double>();
    private Map<Integer, Double> displayStartupTimeRealMap = new HashMap<Integer, Double>();

    private Map<Integer, Double> videoStartupTimeMap = new HashMap<Integer, Double>();

    private static int waitingListSize = 0;

    int vmIndex;
    int temp_key = 0;

    int cloudletSubmittedCount;
    boolean broker_vm_deallocation_flag = false;
    private static boolean dropVideoFlag = true;

    //vm Cost
    private static double vmCost = 0;

    //set up DatacenterCharacteristics;
    public DatacenterCharacteristics characteristics;
    public boolean startupqueue;
    public String sortalgorithm;
    public String schedulingmethod;
    public long rentingTime;

    //flag = 0, cloudlet is from batch queue
    //flag = 1, cloudlet is from new arrival queue
    private int switch_flag = 0;

    public VideoFogBroker(String name) throws Exception {
        super(name);
    }

    @Override
    public void startEntity() {
        // TODO Auto-generated method stub
    }

    /**
     * Processes events available for this Broker.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // Resource characteristics request
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                processResourceCharacteristicsRequest(ev);
                break;
            // Resource characteristics answer
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                processResourceCharacteristics(ev);
                break;
            // VM Creation answer
            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev);
                break;
            // A finished cloudlet returned
            case CloudSimTags.CLOUDLET_RETURN:
                processCloudletReturn(ev);
                break;
            // if the simulation finishes
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;

            /**
             * @override add a new tag CLOUDLET_SUBMIT_RESUME to broker updating
             * cloudletwaitinglist in VideoSegmentScheduler, whenever a vm's
             * waitinglist is smaller than 2, it will add a event in the broker,
             * so that broker can keep send this vm the rest of cloudlet in its
             * batch queue
             *
             */
            case CLOUDLET_SUBMIT_RESUME:
                submitCloudlets();
                break;

            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    @Override
    public void shutdownEntity() {
        // TODO Auto-generated method stub

    }

    public void submitCloudlets() {
        /**
         * Check if we can insert the first cloudlet in the new arrival queue to
         * the front of the batch queue. 1. calculate the minimum completion
         * time when cloudlet_new insert in the front of batch queue 2. compare
         * this minimum completion time to the deadline of the cloudlet in the
         * front of batch queue
         */
        //cloudlet will be sent to vm
        VideoSegment cloudlet_video;
        VideoSegment cloudlet_new;
        VideoSegment cloudlet_batch;

        //VideoStreams vstream = new VideoStreams();
        TranscodingVm vm_transcoding;

        int vmIndex = 0;
        for (Cloudlet cloudlet : getCloudletList()) {
            Vm vm;
            // if user didn't bind this cloudlet and it has not been executed yet
            if (cloudlet.getVmId() == -1) {
                vm = getVmsCreatedList().get(vmIndex);
            } else { // submit to the specific vm
                vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
                if (vm == null) { // vm was not created
                    Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
                            + cloudlet.getCloudletId() + ": bount VM not available");
                    continue;
                }
            }

            Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
                    + cloudlet.getCloudletId() + " to VM #" + vm.getId());
            cloudlet.setVmId(vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
            getCloudletSubmittedList().add(cloudlet);
        }

        // remove submitted cloudlets from waiting list
        for (Cloudlet c : getCloudletSubmittedList()) {
            getCloudletList().remove(c);
        }
    }

    /**
     * This method is used to send to the broker the list of cloudlets.
     *
     * @param list the list
     * @pre list !=null
     * @post $none
     */
    public void submitCloudletList(List<? extends Cloudlet> cloudletBatchQueue, List<? extends Cloudlet> cloudletNewArrivalQueue) {
        //Before submit new cloudlet list, delete those who have already been submitted. 
        //List<? extends Cloudlet> cloudletNewArrivalQueue_temp = Collections.synchronizedList(new ArrayList<Cloudlet>());
        List<Cloudlet> cloudletBatchQueue_temp = Collections.synchronizedList(new ArrayList<Cloudlet>());
        List<Cloudlet> cloudletNewQueue_temp = Collections.synchronizedList(new ArrayList<Cloudlet>());

        //cloudletNewArrivalQueue_temp = cloudletNewArrivalQueue;
        cloudletBatchQueue_temp.addAll(cloudletBatchQueue);
        cloudletNewQueue_temp.addAll(cloudletNewArrivalQueue);

        for (Cloudlet cl : cloudletBatchQueue_temp) {
            if (getCloudletSubmittedList().contains(cl)) {
                cloudletBatchQueue.remove(cl);
            }
        }

        //Delete duplicated cloudlets which are already in the batch queue.
        for (Cloudlet cl : cloudletBatchQueue_temp) {
            if (getCloudletList().contains(cl)) {
                cloudletBatchQueue.remove(cl);
            }
        }

        for (Cloudlet cl : cloudletNewQueue_temp) {
            if (getCloudletSubmittedList().contains(cl)) {
                cloudletNewArrivalQueue.remove(cl);
            }
        }

        //getCloudletNewList().clear();
        getCloudletList().addAll(cloudletBatchQueue);
        getCloudletNewList().addAll(cloudletNewArrivalQueue);
        ArrayList<Integer> newcloudlets = new ArrayList<Integer>();

        for (int i = 0; i < cloudletNewList.size(); i++) {
            newcloudlets.add(cloudletNewList.get(i).getCloudletId());
        }

        System.out.println(Thread.currentThread().getName() + "*****New arrival queue Video ID_" + videoId + ": " + newcloudlets + " **********");

        System.out.println("**********************The size of batch queue is: " + getCloudletList().size() + " **************");
    }

    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getCloudletNewList() {
        return (List<T>) cloudletNewList;
    }

}
