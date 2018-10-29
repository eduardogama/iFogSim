/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd.cs;

import org.fog.entities.Sensor;
import org.fog.utils.GeoLocation;
import org.fog.utils.distribution.Distribution;

/**
 *
 * @author futebol
 */
public class MySensor extends Sensor{
    
    public MySensor(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, Distribution transmitDistribution, int cpuLength, int nwLength, String tupleType, String destModuleName) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation, transmitDistribution, cpuLength, nwLength, tupleType, destModuleName);
    }
    
}
