/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 *
 * @author futebol
 */
public class PrintOutputFile {

    public static String file_name = "";

    public static void AddtoFile(String msg) {
        try {
            java.util.Date date = new java.util.Date();
            if (file_name == "") {
                file_name = "/home/futebol/simulation-tools/iFogSim/output/ifogsim_log-" + date.getTime() + ".txt";
            }
            File file = new File(file_name);

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);

            fw.write(msg);
            fw.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void CustomtoString(SimEvent ev) {
        ArrayList<String> type = new ArrayList<String>();
        type.add("ENULL");
        type.add("SEND");
        type.add("HOLD_DONE");
        type.add("CREATE");

        String msg = " Future event queue\n: -->Event Type = " + type.get(ev.getType())
                + ";Event tag = " + CloudSimTags.TagText(ev.getTag()) + "; Source = "
                + CloudSim.getEntity(ev.getSource()).getName() + "; Destination = "
                + CloudSim.getEntity(ev.getDestination()).getName() + "; Time = " + ev.eventTime()
                + "; endwating time = " + ev.endWaitingTime();

        if (ev.getData() != null) {
            msg += "; data = " + ev.getData().toString();
        }
        msg += "\n\n";

        AddtoFile(msg);
    }

}
