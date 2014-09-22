package fr.ortolang.diffusion.admin.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.ortolang.diffusion.admin.JmxTools;

@SuppressWarnings("serial")
public class ServerStateServlet extends HttpServlet {
	
	private Logger logger = Logger.getLogger(ServerStateServlet.class.getName());

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.log(Level.FINE, "ServerState Servlet Called");
		
		MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);
		RuntimeInformation infos = new RuntimeInformation();
		try {
            ObjectName runtimeOName = new ObjectName("java.lang:type=Runtime");
            infos.setStartTime(JmxTools.getLongAttr(server, runtimeOName, "StartTime"));
            infos.setUptime(JmxTools.getLongAttr(server, runtimeOName, "Uptime"));
            infos.setVmVendor(JmxTools.getStringAttr(server,runtimeOName,"VmVendor"));

            ObjectName osOName = new ObjectName("java.lang:type=OperatingSystem");
            infos.setOsName(JmxTools.getStringAttr(server, osOName, "Name"));
            infos.setOsVersion(JmxTools.getStringAttr(server, osOName, "Version"));

            if(!infos.getVmVendor().startsWith("IBM Corporation")){
                infos.setTotalPhysicalMemorySize(JmxTools.getLongAttr(server, osOName, "TotalPhysicalMemorySize"));
                infos.setCommittedVirtualMemorySize(JmxTools.getLongAttr(server, osOName, "CommittedVirtualMemorySize"));
                infos.setFreePhysicalMemorySize(JmxTools.getLongAttr(server, osOName, "FreePhysicalMemorySize"));
                infos.setFreeSwapSpaceSize(JmxTools.getLongAttr(server, osOName, "FreeSwapSpaceSize"));
                infos.setTotalSwapSpaceSize(JmxTools.getLongAttr(server, osOName, "TotalSwapSpaceSize"));
                infos.setProcessCpuTime(JmxTools.getLongAttr(server, osOName, "ProcessCpuTime"));
                infos.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
            } else {
                infos.setTotalPhysicalMemorySize(JmxTools.getLongAttr(server, osOName, "TotalPhysicalMemory"));
                infos.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
            }

            if (JmxTools.hasAttribute(server, osOName, "OpenFileDescriptorCount")
                    && JmxTools.hasAttribute(server, osOName, "MaxFileDescriptorCount")) {

                infos.setOpenFDCount(JmxTools.getLongAttr(server, osOName, "OpenFileDescriptorCount"));
                infos.setMaxFDCount(JmxTools.getLongAttr(server, osOName, "MaxFileDescriptorCount"));
            }
            
            request.setAttribute("runtime", infos);
        } catch (Exception e) {
            logger.log(Level.INFO, "OS information is unavailable");
        }
		
		getServletContext().getRequestDispatcher("/WEB-INF/pages/serverstate.jsp").forward(request, response);
	}

}
