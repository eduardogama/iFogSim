/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd.cs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.util.Pair;
import static org.cloudbus.cloudsim.sdn.example.SDNBroker.appId;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogUtils;
import org.fog.utils.GeoCoverage;

/**
 *
 * @author futebol
 */
public class MyApplication extends Application{

    private Map<Integer, Map<String, Double>> deadlineInfo;
    private Map<Integer, Map<String, Integer>> additionalMipsInfo;

    public MyApplication(String appId, List<AppModule> modules, List<AppEdge> edges, List<AppLoop> loops, GeoCoverage geoCoverage) {
        super(appId, modules, edges, loops, geoCoverage);
    }

    private MyApplication(String appId, int userId) {
        super(appId,userId);
    }

    public Map<Integer, Map<String, Integer>> getAdditionalMipsInfo() {
        return additionalMipsInfo;
    }

    public void setAdditionalMipsInfo(Map<Integer, Map<String, Integer>> additionalMipsInfo) {
        this.additionalMipsInfo = additionalMipsInfo;
    }

    public void setDeadlineInfo(Map<Integer, Map<String, Double>> deadlineInfo) {
        this.deadlineInfo = deadlineInfo;
    }

    public Map<Integer, Map<String, Double>> getDeadlineInfo() {
        return deadlineInfo;
    }

    public void addAppModule(String moduleName, int ram, int mips, long size, long bw) {
        String vmm = "Xen";
        AppModule module = new AppModule(FogUtils.generateEntityId(), moduleName, appId, userId, mips, 
                ram, bw, size, vmm, new TupleScheduler(mips, 1), new HashMap<Pair<String, String>, SelectivityModel>());
        getModules().add(module);
    }
    
    public static MyApplication createMyApplication(String appId, int userId) {
        return new MyApplication(appId, userId);
    }
}
