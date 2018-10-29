package org.fog_impl;

public class MapInstanceToVideoLength {

    private long videoLength;

    public MapInstanceToVideoLength() {

    }

    public Long getVideoLength(String ec2Type, Long transcodingTime) {

        //Based on the EC2 types, the MIPS are different, map different GOP length based on intances' MIPs
        if (ec2Type.equalsIgnoreCase("c4.xlarge")) {
            videoLength = transcodingTime * 8000;
        } else if (ec2Type.equalsIgnoreCase("r3.xlarge")) {
            videoLength = transcodingTime * 6000;
        } else if (ec2Type.equalsIgnoreCase("t2.small")) {
            videoLength = transcodingTime * 1100;
        } else if (ec2Type.equalsIgnoreCase("g2.2xlarge")) {
            videoLength = transcodingTime * 13000;
        } else if (ec2Type.equalsIgnoreCase("m4.large")) {
            videoLength = transcodingTime * 4000;
        } else {
            videoLength = transcodingTime * 8000;
        }
        return videoLength;
    }

}
