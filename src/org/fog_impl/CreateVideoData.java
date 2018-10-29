/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog_impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author futebol
 */
public class CreateVideoData {

    private String transcodedTo;
    private ArrayList<Integer> videoIdList = new ArrayList<Integer>();
    private ArrayList<Integer> deviationVideoTranscodingTimeList = new ArrayList<Integer>();
    private ArrayList<Integer> videoPtsList = new ArrayList<Integer>();
    private ArrayList<Integer> videoInputSizeList = new ArrayList<Integer>();
    private ArrayList<Integer> videoOutputSizeList = new ArrayList<Integer>();
    private ArrayList<Integer> stdList = new ArrayList<Integer>();
    private ArrayList<HashMap<String, Long>> videoTranscodingTimeMapList = new ArrayList<HashMap<String, Long>>();

    public CreateVideoData() {

    }

    public String getTranscodedTo() {
        return transcodedTo;
    }

    public void setTranscodedTo(String transcodedTo) {
        this.transcodedTo = transcodedTo;
    }

    public ArrayList<HashMap<String, Long>> getVideoengthMapList() {
        return videoTranscodingTimeMapList;
    }

    public void addToVideoLengthMapList(String ec2Type, Long transcodingTime) {
        HashMap<String, Long> tempMap = new HashMap<String, Long>();
        long videoLength;

        MapInstanceToVideoLength mig = new MapInstanceToVideoLength();
        videoLength = mig.getVideoLength(ec2Type, transcodingTime);

        tempMap.put(ec2Type, videoLength);
        this.videoTranscodingTimeMapList.add(tempMap);
    }

    public ArrayList<Integer> getVideoIdList() {
        return videoIdList;
    }

    public void addToVideoIdList(int gopId) {
        this.videoIdList.add(gopId);
    }

    public ArrayList<Integer> getDeviationVideoTranscodingTimeList() {
        return deviationVideoTranscodingTimeList;
    }

    public void addToDeviationVideoTranscodingTimeList(int deviationVideoTranscodingTime) {
        this.deviationVideoTranscodingTimeList.add(deviationVideoTranscodingTime);
    }

    public ArrayList<Integer> getVideoPtsList() {
        return videoPtsList;
    }

    public void addToVideoPtsList(int gopPts) {
        this.videoPtsList.add(gopPts);
    }

    public ArrayList<Integer> getVideoInputSizeList() {
        return videoInputSizeList;
    }

    public void addToVideoInputSizeList(int gopInputSize) {
        this.videoInputSizeList.add(gopInputSize);
    }

    public ArrayList<Integer> getVideoOutputSizeList() {
        return videoOutputSizeList;
    }

    public void addToVideoOutputSizeList(int gopOutputSize) {
        this.videoOutputSizeList.add(gopOutputSize);
    }

    public ArrayList<Integer> getStdList() {
        return stdList;
    }

    public void addToStdList(int std) {
        this.stdList.add(std);
    }

    public void ParseCloudlet(File videoDataFile) {

        /**
         * Combine different instance's transcoding time into a map
         * <instance type, transcoding time>
         */
        //File videoDataFile = new File(cloudletUrl);
        BufferedReader br = null;

        try {
            FileReader fr = new FileReader(videoDataFile);
            br = new BufferedReader(fr);

            String sCurrentLine;
            String splitterFlag = "Resolution";
            boolean flag = true;
            int index = 1;
            long videoLength = 0;

            if (videoDataFile.isFile() && videoDataFile.getName().endsWith(".txt")) {
                while ((sCurrentLine = br.readLine()) != null) {
                    if (sCurrentLine.length() > 0) {
                        
                        System.out.println("> " + sCurrentLine);
                        String[] arr = sCurrentLine.split("\\s+");
                        
                        if (arr[0].equals(splitterFlag)) {
                            if (index == 0) {
                                flag = false;
                            }
                            index = 0;
                            continue;
                        } else if (flag) {
                            setTranscodedTo(arr[0]);
                            addToVideoLengthMapList(arr[1], Long.parseLong(arr[2]));
                            addToVideoIdList(Integer.parseInt(arr[1]));
                            addToVideoPtsList(Integer.parseInt(arr[3]));
                            addToVideoInputSizeList(Integer.parseInt(arr[4]));
                            addToVideoOutputSizeList(Integer.parseInt(arr[5]));
                        } else {
                            MapInstanceToVideoLength mitl = new MapInstanceToVideoLength();
                            videoLength = mitl.getVideoLength(arr[1], Long.parseLong(arr[3]));

                            getVideoengthMapList().get(index).put(arr[1], videoLength);
                            index++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
}
