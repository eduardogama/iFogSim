/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package devices;

/**
 *
 * @author futebol
 */
public class InstanceType {

    /**
     * mips - MIPS ram - RAM upBw - uplink bandwidth downBw - downlink bandwidth
     * level - hierarchy level of the device ratePerMips - cost rate per MIPS
     * used busyPower idlePower 
    *
     */
    private String instanceType = null;
    private int instanceMips;
    private int instanceRam;
    private int instanceUpBw;
    private int instanceDownBw;
    private long instanceBw;
    private int instancePesNumber;
    private long instanceStorageSize;
    private double instanceCost;

    public InstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public int getInstanceMips() {
        if (instanceType.equals("cellphone")) {
            this.instanceMips = 50000;
        } else {
            this.instanceMips = 1000;
        }

        return instanceMips;
    }

    public int getInstanceRam() {
        if (instanceType.equals("cellphone")) {
            this.instanceRam = 2048;
        } else {
            this.instanceRam = 1000;
        }
        return instanceRam;
    }

    public long getInstanceBw() {

        return instanceBw;
    }

    public int getInstanceUpBw() {
        if (instanceType.equals("cellphone")) {
            this.instanceUpBw = 1000;    
        } else {
            this.instanceUpBw = 10000;
        }
        return instanceUpBw;
    }

    public int getInstanceDownBw() {
        if (instanceType.equals("cellphone")) {
            this.instanceDownBw = 1000;
        } else {
            this.instanceDownBw = 270;
        }
        return instanceDownBw;
    }

    public int getInstancePesNumber() {
        if (instanceType.equals("cellphone")) {
            this.instancePesNumber = 2;
        } else {
            this.instancePesNumber = 2;
        }
        return instancePesNumber;
    }

    public long getInstanceStorageSize() {
        if (instanceType.equals("cellphone")) {
            this.instanceStorageSize = 1000000; // host storage
        } else {
            this.instanceStorageSize = 1000000; 
        }
        return instanceStorageSize;
    }

    public double getInstanceCost() {
        if (instanceType.equals("cellphone")) {
            this.instanceCost = 1000;
        } else {
            this.instanceCost = 270;
        }
        return instanceCost;
    }

}
