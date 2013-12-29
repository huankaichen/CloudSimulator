/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package entropycloud.environment;

/**
 *
 * @author hkchen
 */

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class environmentSetup {
    /* Task List */
    private static List<Cloudlet> cloudletList;
    /* Virtual Machine List */
    private static List<Vm> vmlist;
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        Log.printLine("Starting Cloud Simulation");
        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            
            /* Step 1: Initial Cloudsim Library */
            CloudSim.init(num_user, calendar, trace_flag);
            
            /* Step 2: Create Datacenter, contains Hosts with hostID, ram:2048, Storage:1000000, Bandwidth:10000, PE speed mips:1000, Timeshared Scheduler */
            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            
            /* Step 3: Create Broker */
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();
            Log.printLine("BrokerId is " + brokerId);
            
            /* Step 4: Create VM */
            vmlist = new ArrayList<Vm>();

            int vmid = 0;
            int mips = 1000;
            long size = 10000; // image size (MB)
            int ram = 512; // vm memory (MB)
            long bw = 1000;
            int pesNumber = 1; // number of cpus
            String vmm = "Xen"; // VMM name

            Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

            vmlist.add(vm);
            broker.submitVmList(vmlist);
            
            /* Step 5: Create Cloud Task */
            cloudletList = new ArrayList<Cloudlet>();

            int id = 0;
            long length = 400000; // Number of instructions? Time = Length/MIPS =400 Sec
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();
            
            for (int i=0;i<3;i++){
            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            cloudlet.setVmId(vmid);

            cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);
            
            /* Step 6: Start and Stop the simulation */
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            
            /* Final Report */
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);
        } 
        catch (Exception e) {
			Log.printLine("Unwanted errors happen");
	}
    }
    
    private static Datacenter createDatacenter(String name) {

            // Here are the steps needed to create a PowerDatacenter:
            // 1. We need to create a list to store
            // our machine
            List<Host> hostList = new ArrayList<Host>();

            // 2. A Machine contains one or more PEs or CPUs/Cores.
            // In this example, it will have only one core.
            List<Pe> peList = new ArrayList<Pe>();

            //Millions of Instructions per Second, Execution Speed of CPU
            int mips = 1000;

            // 3. Create PEs and add these into a list.
            peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

            // 4. Create Host with its id and list of PEs and add them to the list
            // of machines
            int hostId = 0;
            int ram = 2048; // host memory (MB)
            long storage = 1000000; // host storage
            int bw = 10000;

            hostList.add(
                    new Host(
                            hostId,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw),
                            storage,
                            peList,
                            new VmSchedulerTimeShared(peList)
                    )
            ); // This is our machine

            // 5. Create a DatacenterCharacteristics object that stores the
            // properties of a data center: architecture, OS, list of
            // Machines, allocation policy: time- or space-shared, time zone
            // and its price (G$/Pe time unit).
            String arch = "x86"; // system architecture
            String os = "Linux"; // operating system
            String vmm = "Xen";
            double time_zone = 10.0; // time zone this resource located
            double cost = 3.0; // the cost of using processing in this resource
            double costPerMem = 0.05; // the cost of using memory in this resource
            double costPerStorage = 0.001; // the cost of using storage in this
                                                                            // resource
            double costPerBw = 0.0; // the cost of using bw in this resource
            LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
                                                                                                    // devices by now

            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                            arch, os, vmm, hostList, time_zone, cost, costPerMem,
                            costPerStorage, costPerBw);

            // 6. Finally, we need to create a PowerDatacenter object.
            Datacenter datacenter = null;
            try {
                    datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
            } catch (Exception e) {
                    e.printStackTrace();
            }

            return datacenter;
    }
    
    // Create Broker
    private static DatacenterBroker createBroker() {
            DatacenterBroker broker = null;
            try {
                    broker = new DatacenterBroker("Broker");
            } catch (Exception e) {
                    e.printStackTrace();
                    return null;
            }
            return broker;
    } 
    
    private static void printCloudletList(List<Cloudlet> list) {
            int size = list.size();
            Cloudlet cloudlet;

            String indent = "    ";
            Log.printLine();
            Log.printLine("========== OUTPUT ==========");
            Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                            + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
                            + "Start Time" + indent + "Finish Time");

            DecimalFormat dft = new DecimalFormat("###.##");
            for (int i = 0; i < size; i++) {
                    cloudlet = list.get(i);
                    Log.print(indent + cloudlet.getCloudletId() + indent + indent);

                    if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                            Log.print("SUCCESS");

                            Log.printLine(indent + indent + cloudlet.getResourceId()
                                            + indent + indent + indent + cloudlet.getVmId()
                                            + indent + indent
                                            + dft.format(cloudlet.getActualCPUTime()) + indent
                                            + indent + dft.format(cloudlet.getExecStartTime())
                                            + indent + indent
                                            + dft.format(cloudlet.getFinishTime()));
                    }
            }
    }
}
