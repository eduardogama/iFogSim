/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phd;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog_impl.VideoFogBroker;

/**
 *
 * @author futebol
 */
public class MapInstance {
    
    private String name;
    private String appId;
    private Application app;
    private VideoFogBroker broker;
    private FogDevice device;
    private Actuator actuator;
    
    public MapInstance(String name, String appId, Application app, VideoFogBroker broker, FogDevice device, Actuator actuator) {
        this.name = name;
        this.appId = appId;
        this.app = app;
        this.broker = broker;
        this.device = device;
        this.actuator = actuator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Application getApp() {
        return app;
    }

    public void setApp(Application app) {
        this.app = app;
    }

    public VideoFogBroker getBroker() {
        return broker;
    }

    public void setBroker(VideoFogBroker broker) {
        this.broker = broker;
    }

    public FogDevice getDevice() {
        return device;
    }

    public void setDevice(FogDevice device) {
        this.device = device;
    }

    public Actuator getActuator() {
        return actuator;
    }

    public void setActuator(Actuator actuator) {
        this.actuator = actuator;
    }
    
}
