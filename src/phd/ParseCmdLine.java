package phd;

import org.kohsuke.args4j.Option;

public class ParseCmdLine {

    @Option(name = "-property", usage = "property file url")
    private String propertiesFileURL = null;

    @Option(name = "-input", usage = "input data folder url")
    private String inputdataFolderURL = null;

    @Option(name = "-output", usage = "output file url")
    private String outputdataFileURL = null;

    @Option(name = "-gopdelayoutput", usage = "gopdelay output file url")
    private String gopdelayoutput = null;

    @Option(name = "-sortalgorithm", usage = "sorting algorithm")
    private String sortalgorithm = null;

    @Option(name = "-schedulingmethod", usage = "scheduling method")
    private String schedulingmethod = null;

    @Option(name = "-startupqueue", usage = "with start up queue or not")
    private boolean startupqueue;

    @Option(name = "-dropflag", usage = "with droping or not")
    private boolean dropflag;

    @Option(name = "-stqprediction", usage = "use startup queue prediction or not")
    private boolean stqprediction;

    @Option(name = "-videonum", usage = "video number")
    private Integer jobNum = 0;

    @Option(name = "-vmqueue", usage = "vm local queue size")
    private Integer waitinglist_max = 0;

    @Option(name = "-vmNum", usage = "vm number")
    private Integer vmNum = 0;

    @Option(name = "-clusterType", usage = "cluster type")
    private String clusterType = null;

    @Option(name = "-vmType", usage = "vm type")
    private String vmType = null;

    @Option(name = "-vmfrequency", usage = "video number")
    private Integer frequency = 0;

    @Option(name = "-goplength", usage = "estimated gop length")
    private String gopLength = null;

    @Option(name = "-upthreshold", usage = "deadline miss rate upthredshold")
    private Double upthredshold = 0.0;

    @Option(name = "-lowthreshold", usage = "deadline miss rate lowthredshold")
    private Double lowthredshold = 0.0;

    @Option(name = "-testPeriod", usage = "test time period")
    private Double testPeriod = 0.0;

    @Option(name = "-rentingTime", usage = "vm renting time")
    private Long rentingTime = (long) 0;

    @Option(name = "-seedshift", usage = "change seed")
    private Integer seedshift = 0;
    
    @Option(name = "-appNum", usage = "app number")
    private Integer appNum = 0;    
    
    @Option(name = "-cloudNum", usage = "cloud number")
    private Integer cloudNum = 0;    
    
    @Option(name = "-apNum", usage = "ap number")
    private Integer apNum = 0;    
    
    @Option(name = "-gwNum", usage = "gw number")
    private Integer gwNum = 0;    
    
    @Option(name = "-euNum", usage = "eu number")
    private Integer euNum = 0;

    public String getGopdelayoutput() {
        return gopdelayoutput;
    }

    public void setGopdelayoutput(String gopdelayoutput) {
        this.gopdelayoutput = gopdelayoutput;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public String getVmType() {
        return vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
    }

    public Boolean getStqPrediction() {
        return stqprediction;
    }

    public Boolean getDropFlag() {
        return dropflag;
    }

    public Double getTestPeriod() {
        return testPeriod;
    }

    public Long getRentingTime() {
        return rentingTime;
    }

    public Double getUpThredshold() {
        return upthredshold;
    }

    public Double getLowThreshold() {
        return lowthredshold;
    }

    public String getEstimatedGopLength() {
        return gopLength;
    }

    public String getPropertiesFileURL() {
        return propertiesFileURL;
    }

    public String getInputdataFolderURL() {
        return inputdataFolderURL;
    }

    public String getOutputdataFileURL() {
        return outputdataFileURL;
    }

    public String getSortAlgorithm() {
        return sortalgorithm;
    }

    public String getSchedulingMethod() {
        return schedulingmethod;
    }

    public boolean getStarupQueue() {
        return startupqueue;
    }

    public int getVideoNum() {
        return jobNum;
    }

    public int getVmQueueSize() {
        return waitinglist_max;
    }

    public int getVmNum() {
        return vmNum;
    }

    public int getVmFrequency() {
        return frequency;
    }

    public int getSeedShift() {
        return seedshift;
    }

    public Integer getAppNum() {
        return appNum;
    }

    public void setAppNum(Integer appNum) {
        this.appNum = appNum;
    }

    public Integer getCloudNum() {
        return cloudNum;
    }

    public void setCloudNum(Integer cloudNum) {
        this.cloudNum = cloudNum;
    }

    public Integer getApNum() {
        return apNum;
    }

    public void setApNum(Integer apNum) {
        this.apNum = apNum;
    }

    public Integer getGwNum() {
        return gwNum;
    }

    public void setGwNum(Integer gwNum) {
        this.gwNum = gwNum;
    }

    public Integer getEuNum() {
        return euNum;
    }

    public void setEuNum(Integer euNum) {
        this.euNum = euNum;
    }
    
    

    /**
     * If you want to get the args-Array from the command line use the signature
     * <tt>run(String[] args)</tt>. But then there must not be a run() because
     * that is executed prior to this.
     *
     * @param args The arguments as specified on the command line
     */
    public void run(String[] args) {
        System.out.println("SampleStarter.run(String[])");
        System.out.println("- args.length: " + args.length);
        
        for (String arg : args) {
            System.out.println("  - " + arg);
        }
        System.out.println(this);
    }

}