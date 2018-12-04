package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import oshi.hardware.CentralProcessor.TickType;
import oshi.json.SystemInfo;
import oshi.json.hardware.*;
import oshi.json.software.os.FileSystem;
import oshi.json.software.os.*;
import oshi.software.os.OperatingSystem.ProcessSort;
import oshi.util.FormatUtil;
import oshi.util.Util;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class SystemDigestCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      if (! context.isLocalAddressLocated()) {
         logger.info("Not running on a host with a deployed node - bypassing.");
         return true;
      }

      try {
         SystemInfo si = new SystemInfo();
         HardwareAbstractionLayer hal = si.getHardware();
         OperatingSystem os = si.getOperatingSystem();
         //Properties props = PropertiesUtil.loadProperties("oshi.json.properties");

         File sysFileJson = new File(context.getTempDir() + SystemProperties.fileSeparator + "system-digest.json");
         OutputStream outputStreamJson = new FileOutputStream(sysFileJson);
         BufferedWriter jsonWriter = new BufferedWriter(new OutputStreamWriter(outputStreamJson));
         String jsonInfo = si.toPrettyJSON();
         context.setAttribute("systemDigest", jsonInfo);
         jsonWriter.write(jsonInfo);
         jsonWriter.close();

         File sysFile = new File(context.getTempDir() + SystemProperties.fileSeparator + "system-digest.txt");
         OutputStream outputStream = new FileOutputStream(sysFile);
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

         printComputerSystem(writer, hal.getComputerSystem());
         writer.newLine();

         printProcessor(writer, hal.getProcessor());
         writer.newLine();

         printMemory(writer, hal.getMemory());
         writer.newLine();

         printCpu(writer, hal.getProcessor());
         writer.newLine();

         printProcesses(writer, os, hal.getMemory());
         writer.newLine();

         printDisks(writer, hal.getDiskStores());
         writer.newLine();

         printFileSystem(writer, os.getFileSystem());
         writer.newLine();

         printNetworkInterfaces(writer, hal.getNetworkIFs());
         writer.newLine();

         printNetworkParameters(writer, os.getNetworkParams());
         writer.newLine();

         writer.close();
      } catch (final Exception e) {
         logger.error("Failed saving system-digest.txt file.", e);
         return false;
      }

      logger.info("Finished querying SysInfo.");

      return true;
   }

   private static void printComputerSystem(BufferedWriter writer, final ComputerSystem computerSystem) throws Exception{

      writer.write("Computer System");
      writer.newLine();
      writer.write("----------------");
      writer.newLine();

      writer.write("manufacturer: " + computerSystem.getManufacturer());
      writer.newLine();

      writer.write("model: " + computerSystem.getModel());
      writer.newLine();

      writer.write("serialnumber: " + computerSystem.getSerialNumber());
      writer.newLine();

      final Firmware firmware = computerSystem.getFirmware();
      writer.write("firmware:");
      writer.newLine();

      writer.write("  manufacturer: " + firmware.getManufacturer());
      writer.newLine();

      writer.write("  name: " + firmware.getName());
      writer.newLine();

      writer.write("  description: " + firmware.getDescription());
      writer.newLine();

      writer.write("  version: " + firmware.getVersion());
      writer.newLine();

      writer.write("  release date: " + (firmware.getReleaseDate() == null ? "unknown"
         : firmware.getReleaseDate() == null ? "unknown" : firmware.getReleaseDate()));
      writer.newLine();

      final Baseboard baseboard = computerSystem.getBaseboard();
      writer.write("baseboard:");
      writer.newLine();

      writer.write("  manufacturer: " + baseboard.getManufacturer());

      writer.write("  model: " + baseboard.getModel());
      writer.newLine();

      writer.write("  version: " + baseboard.getVersion());
      writer.newLine();

      writer.write("  serialnumber: " + baseboard.getSerialNumber());
      writer.newLine();

   }

   private static void printProcessor(BufferedWriter writer, CentralProcessor processor) throws Exception {

      writer.write("Processors");
      writer.newLine();
      writer.write("----------");
      writer.newLine();
      writer.write(" " + processor.getPhysicalPackageCount() + " physical CPU package(s)");
      writer.newLine();

      writer.write(" " + processor.getPhysicalProcessorCount() + " physical CPU core(s)");
      writer.newLine();

      writer.write(" " + processor.getLogicalProcessorCount() + " logical CPU(s)");
      writer.newLine();

      writer.write("Identifier: " + processor.getIdentifier());
      writer.newLine();

      writer.write("ProcessorID: " + processor.getProcessorID());
      writer.newLine();

   }

   private static void printProcesses(BufferedWriter writer, OperatingSystem os, GlobalMemory memory) throws Exception{
      writer.write("Processes");
      writer.newLine();
      writer.write("----------");
      writer.newLine();
      writer.write("Processes: " + os.getProcessCount() + ", Threads: " + os.getThreadCount());
      writer.newLine();

      // Sort by highest CPU
      List<OSProcess> procs = Arrays.asList(os.getProcesses(os.getProcessCount(), ProcessSort.CPU));
      int sz = procs.size();
      writer.write("PID     %CPU  %MEM  VSZ      RSS      Name");
      for (int i = 0; i < sz; i++) {
         OSProcess p = procs.get(i);
         writer.write(String.format(" %5d %5.1f %4.1f %9s %9s %s%n", p.getProcessID(),
            100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(),
            100d * p.getResidentSetSize() / memory.getTotal(), FormatUtil.formatBytes(p.getVirtualSize()),
            FormatUtil.formatBytes(p.getResidentSetSize()), p.getName()));
      }
   }

   private static void printMemory(BufferedWriter writer, GlobalMemory memory) throws Exception {
      writer.write("Memory");
      writer.newLine();
      writer.write("-------");
      writer.newLine();

      writer.write("Memory: " + FormatUtil.formatBytes(memory.getAvailable()) + "/"
         + FormatUtil.formatBytes(memory.getTotal()));
      writer.newLine();

      writer.write("Swap used: " + FormatUtil.formatBytes(memory.getSwapUsed()) + "/"
         + FormatUtil.formatBytes(memory.getSwapTotal()));
      writer.newLine();

   }

   private static void printCpu(BufferedWriter writer, CentralProcessor processor) throws Exception{
      writer.write("CPU");
      writer.newLine();
      writer.write("---");
      writer.newLine();
      writer.write("Uptime: " + FormatUtil.formatElapsedSecs(processor.getSystemUptime()));
      writer.newLine();

      writer.write(
         "Context Switches/Interrupts: " + processor.getContextSwitches() + " / " + processor.getInterrupts());
      writer.newLine();


      long[] prevTicks = processor.getSystemCpuLoadTicks();

      writer.write("CPU, IOWait, and IRQ ticks @ 0 sec:" + Arrays.toString(prevTicks));
      writer.newLine();

      // Wait a second...
      Util.sleep(1000);
      long[] ticks = processor.getSystemCpuLoadTicks();

      writer.write("CPU, IOWait, and IRQ ticks @ 1 sec:" + Arrays.toString(ticks));
      writer.newLine();

      long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
      long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
      long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
      long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
      long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
      long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
      long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
      long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
      long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

      writer.write(String.format(
         "User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%% IOwait: %.1f%% IRQ: %.1f%% SoftIRQ: %.1f%% Steal: %.1f%%%n",
         100d * user / totalCpu, 100d * nice / totalCpu, 100d * sys / totalCpu, 100d * idle / totalCpu,
         100d * iowait / totalCpu, 100d * irq / totalCpu, 100d * softirq / totalCpu, 100d * steal / totalCpu));
      writer.newLine();

      writer.write(String.format("CPU load: %.1f%% (counting ticks)%n", processor.getSystemCpuLoadBetweenTicks() * 100));
      writer.write(String.format("CPU load: %.1f%% (OS MXBean)%n", processor.getSystemCpuLoad() * 100));
      double[] loadAverage = processor.getSystemLoadAverage(3);
      writer.write("CPU load averages:" + (loadAverage[0] < 0 ? " N/A" : String.format(" %.2f", loadAverage[0]))
         + (loadAverage[1] < 0 ? " N/A" : String.format(" %.2f", loadAverage[1]))
         + (loadAverage[2] < 0 ? " N/A" : String.format(" %.2f", loadAverage[2])));
      // per core CPU
      StringBuilder procCpu = new StringBuilder("CPU load per processor:");
      double[] load = processor.getProcessorCpuLoadBetweenTicks();
      for (double avg : load) {
         procCpu.append(String.format(" %.1f%%", avg * 100));
      }
      writer.write(procCpu.toString());
      writer.newLine();

   }

   private static void printDisks(BufferedWriter writer, HWDiskStore[] diskStores) throws Exception{
      writer.write("Disks");
      writer.newLine();
      writer.write("-----");
      writer.newLine();

      for (HWDiskStore disk : diskStores) {
         boolean readwrite = disk.getReads() > 0 || disk.getWrites() > 0;
         writer.write(String.format(" %s: (model: %s - S/N: %s) size: %s, reads: %s (%s), writes: %s (%s), xfer: %s ms%n",
            disk.getName(), disk.getModel(), disk.getSerial(),
            disk.getSize() > 0 ? FormatUtil.formatBytesDecimal(disk.getSize()) : "?",
            readwrite ? disk.getReads() : "?", readwrite ? FormatUtil.formatBytes(disk.getReadBytes()) : "?",
            readwrite ? disk.getWrites() : "?", readwrite ? FormatUtil.formatBytes(disk.getWriteBytes()) : "?",
            readwrite ? disk.getTransferTime() : "?"));
         HWPartition[] partitions = disk.getPartitions();
         if (partitions == null) {
            continue;
         }
         for (HWPartition part : partitions) {
            writer.write(String.format(" |-- %s: %s (%s) Maj:Min=%d:%d, size: %s%s%n", part.getIdentification(),
               part.getName(), part.getType(), part.getMajor(), part.getMinor(),
               FormatUtil.formatBytesDecimal(part.getSize()),
               part.getMountPoint().isEmpty() ? "" : " @ " + part.getMountPoint()));
         }
      }
   }

   private static void printFileSystem(BufferedWriter writer,  FileSystem fileSystem) throws Exception{
      writer.write("File System");
      writer.newLine();
      writer.write("-------------");
      writer.newLine();
      writer.write(String.format(" File Descriptors: %d/%d%n", fileSystem.getOpenFileDescriptors(),
         fileSystem.getMaxFileDescriptors()));

      OSFileStore[] fsArray = fileSystem.getFileStores();
      for (OSFileStore fs : fsArray) {
         long usable = fs.getUsableSpace();
         long total = fs.getTotalSpace();
         writer.write(String.format(" %s (%s) [%s] %s of %s free (%.1f%%) is %s and is mounted at %s%n", fs.getName(),
            fs.getDescription().isEmpty() ? "file system" : fs.getDescription(), fs.getType(),
            FormatUtil.formatBytes(usable), FormatUtil.formatBytes(fs.getTotalSpace()), 100d * usable / total,
            fs.getVolume(), fs.getMount()));
      }
   }

   private static void printNetworkParameters(BufferedWriter writer, NetworkParams networkParams) throws Exception{
      writer.write("Network Parameters");
      writer.newLine();
      writer.write("----------------");
      writer.newLine();
      writer.write(String.format(" Host name: %s%n", networkParams.getHostName()));
      writer.write(String.format(" Domain name: %s%n", networkParams.getDomainName()));
      writer.write(String.format(" DNS servers: %s%n", Arrays.toString(networkParams.getDnsServers())));
      writer.write(String.format(" IPv4 Gateway: %s%n", networkParams.getIpv4DefaultGateway()));
      writer.write(String.format(" IPv6 Gateway: %s%n", networkParams.getIpv6DefaultGateway()));

   }

   private static void printNetworkInterfaces(BufferedWriter writer,NetworkIF[] networkIFs)  throws Exception {
      writer.write("Network interfaces");
      writer.newLine();
      writer.write("----------------");
      writer.newLine();

      for (NetworkIF net : networkIFs) {
         writer.write(String.format(" Name: %s (%s)%n", net.getName(), net.getDisplayName()));
         writer.write(String.format("   MAC Address: %s %n", net.getMacaddr()));
         writer.write(String.format("   MTU: %s, Speed: %s %n", net.getMTU(), FormatUtil.formatValue(net.getSpeed(), "bps")));
         writer.write(String.format("   IPv4: %s %n", Arrays.toString(net.getIPv4addr())));
         writer.write(String.format("   IPv6: %s %n", Arrays.toString(net.getIPv6addr())));
         boolean hasData = net.getBytesRecv() > 0 || net.getBytesSent() > 0 || net.getPacketsRecv() > 0
            || net.getPacketsSent() > 0;
         writer.write(String.format("   Traffic: received %s/%s%s; transmitted %s/%s%s %n",
            hasData ? net.getPacketsRecv() + " packets" : "?",
            hasData ? FormatUtil.formatBytes(net.getBytesRecv()) : "?",
            hasData ? " (" + net.getInErrors() + " err)" : "",
            hasData ? net.getPacketsSent() + " packets" : "?",
            hasData ? FormatUtil.formatBytes(net.getBytesSent()) : "?",
            hasData ? " (" + net.getOutErrors() + " err)" : ""));

      }
   }


}


