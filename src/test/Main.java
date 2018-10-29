/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.fog_impl.VideoSegment;
import org.fog_impl.VideoStreams;
import phd.transcoder.TranscodingBroker;

/**
 *
 * @author futebol
 */
public class Main {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        int videoId = 0, seedShift = 0, brokerId = 0;
        String inputdataFolderURL = "/home/futebol/simulation-tools/iFogSim/resources/inputdataFile";
        String estimatedGopLength = null;

        boolean startupqueue = true;

        Random random = new Random();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletionService<String> taskCompletionService
                = new ExecutorCompletionService<String>(executorService);

        try {
            ArrayList<Callable<String>> callables = new ArrayList<Callable<String>>();

            int cloudlets = (int) new Main().getRandomNumber(10, 50, random);

            callables.add(new VideoStreams("" + videoId, inputdataFolderURL, startupqueue, seedShift, brokerId, videoId, cloudlets));
            videoId++;

            for (Callable<String> callable : callables) {
                taskCompletionService.submit(callable);
            }

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

        System.out.println("-----------------------");
        // wait until all tasks are finished
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("All tasks are finished!");

        System.out.println("=======================================================");

        VideoStreams vt = new VideoStreams();
        List<VideoSegment> cloudletBatchQueue = vt.getBatchQueue();
        List<VideoSegment> cloudletNewArrivalQueue = vt.getNewArrivalQueue();

        TranscodingBroker broker = null;
        
        try {
            broker = new TranscodingBroker("TranscodingBroker",
                    new DatacenterCharacteristics("x86",
                            "Linux",
                            "Xeon",
                            null,
                            10.0,
                            0.0000036,
                            0.05,
                            0.03,
                            0.1),
                    "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        broker.submitCloudletList(cloudletBatchQueue, cloudletNewArrivalQueue);
        
    }

    //Generate random number for random length cloudlets
    public long getRandomNumber(int aStart, int aEnd, Random aRandom) {
        if (aStart > aEnd) {
            throw new IllegalArgumentException("Start cannot exceed End.");
        }
        //get the range, casting to long to avoid overflow problems
        long range = (long) aEnd - (long) aStart + 1;
        // compute a fraction of the range, 0 <= frac < range
        long fraction = (long) (range * aRandom.nextDouble());

        return (long) (fraction + aStart);
    }
}
