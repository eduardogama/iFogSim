/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd.cs;

import org.fog.entities.Actuator;
import org.fog.utils.GeoLocation;

/**
 *
 * @author futebol
 */
public class MyActuator extends Actuator{
    
    public MyActuator(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, String actuatorType, String srcModuleName) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation, actuatorType, srcModuleName);
    }
    
}
