/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd.mobility;

import java.util.HashMap;
import java.util.Map;
import static javax.xml.bind.JAXBIntrospector.getValue;
import org.apache.commons.math3.util.Pair;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 *
 * @author futebol
 */
public class Mobility {

    static Map<Integer, Pair<Double, Integer>> mobilityMap = new HashMap<Integer, Pair<Double, Integer>>();
    static String mobilityDestination = "FogDevice-0";

}
