/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd.transcoder;

import java.util.Map;
import org.cloudbus.cloudsim.Cloudlet;

/**
 *
 * @author futebol
 */
public class EventData {

    private Cloudlet datacloudlet;
    private Map<Integer, Double> dataTotalCompletionTime_vmMap;

    public EventData(Cloudlet cloudlet, Map<Integer, Double> totalCompletionTime_vmMap) {
        this.datacloudlet = cloudlet;
        this.dataTotalCompletionTime_vmMap = totalCompletionTime_vmMap;
    }

    public Cloudlet getDataCloudlet() {
        return datacloudlet;
    }

    public Map<Integer, Double> getDataTimeMap() {
        return dataTotalCompletionTime_vmMap;
    }
}
