package org.cloudbus.mcweb.admissioncontroller;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.mcweb.PredefinedVirtualMachine;
import org.cloudbus.mcweb.ServerFarm;
import org.cloudbus.mcweb.VMType;
import org.cloudbus.mcweb.VirtualMachine;
import org.cloudbus.mcweb.util.Configs;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * 
 *
 */
public class Main {
    public static void main(String[] args) throws Exception {
//        InputStream cloudSiteStreamLoaded = args.length > 0 ? new FileInputStream(args[0]) : Main.class
//                .getResourceAsStream("/cloudsites.properties");
//        InputStream configStreamLoaded = args.length > 1 ? new FileInputStream(args[1]) : Main.class
//                .getResourceAsStream("/config.properties");

        int jettyPort = args.length > 2 ? Integer.parseInt(args[2]) : Configs.DEFAULT_AC_PORT;
        Server jettyServer = new Server(jettyPort);

        try (AutoCloseable serverClosable = ()-> jettyServer.destroy();
                AdmissionController controller = AdmissionController.getInstance()) {
            
            ////// Test configuraiton
            VMType m1Small = new VMType("m1.small", 0.2, 512, 0.7);
            VMType m1Medium = new VMType("m1.medium", 0.5, 1024, 0.9);
            VMType m1Large = new VMType("m1.large", 0.9, 2048, 0.95);

            // vm1 has no user at all times
            List<Number[]> measurements = null;
            measurements = Arrays.asList(new Number[] { 0.1, 0.1, 0 },
                                         new Number[] { 0.1, 0.1, 0 },
                                         new Number[] { 0.1, 0.1, 0 },
                                         new Number[] { 0.1, 0.1, 0 });
            VirtualMachine vm1 = new PredefinedVirtualMachine("127.0.0.1", m1Small, measurements, 10);

            // vm2 has 1 user except for the second fetching
            measurements = Arrays.asList(new Number[] { 0.22, 0.16, 0 },
                                         new Number[] { 0.21, 0.15, 1 },
                                         new Number[] { 0.23, 0.17, 1 },
                                         new Number[] { 0.21, 0.13, 0 });
            VirtualMachine vm2 = new PredefinedVirtualMachine("127.0.0.2", m1Medium, measurements, 20);

            // vm3 has different num of users at all times
            measurements = Arrays.asList(new Number[] { 0.15, 0.10, 0 },
                                         new Number[] { 0.21, 0.25, 1 },
                                         new Number[] { 0.53, 0.54, 3 },
                                         new Number[] { 0.90, 0.75, 0 });
            VirtualMachine vm3 = new PredefinedVirtualMachine("127.0.0.3", m1Medium, measurements, 30);

            // vm4 has different num of users at all times
            measurements = Arrays.asList(new Number[] { 0.15, 0.10, 0 },
                                         new Number[] { 0.31, 0.15, 1 },
                                         new Number[] { 0.53, 0.27, 3 },
                                         new Number[] { 0.90, 0.75, 0 });
            VirtualMachine vm4 = new PredefinedVirtualMachine("127.0.0.4", m1Large, measurements, 40);
            
            // ============= Define the server farm =============
            // Farm fetches every 5 secs
            ServerFarm farm = new ServerFarm(Arrays.asList(vm1, vm2, vm3, vm4), 5000);
            
            AdmissionController.getInstance().configure(new IAdmissionControllerRule() {
                @Override
                public void close() throws Exception {
                    // pass
                }
                
                @Override
                public boolean isEligible(String userToken) {
                    return true;
                }
                
                @Override
                public boolean backOff() {
                    return false;
                }
            }, farm);
            //
            //
            ////////////////////////////////////////////////////////////////////////////////////////
            //
            
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            jettyServer.setHandler(context);

            ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(0);

            // Tells the Jersey Servlet which REST service/class to load.
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                    AdmissionControllerService.class.getCanonicalName());

            jettyServer.start();
            jettyServer.join();
        }
    }
}
