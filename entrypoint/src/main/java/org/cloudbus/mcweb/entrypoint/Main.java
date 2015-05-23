package org.cloudbus.mcweb.entrypoint;

import java.io.FileInputStream;
import java.io.InputStream;

import org.cloudbus.cloudsim.ex.geolocation.geoip2.GeoIP2PingERService;
import org.cloudbus.mcweb.util.Configs;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Starts the entry point web service.
 * 
 * @author nikolay.grozev
 *
 */
public class Main {
    
    /**
     * Starts the entry point service.
     * The arguements should be in the form:
     * 
     * java -jar jar-file.jar [cloudsites-properties-file] [config-properties-file] [port]
     * 
     * The resulting server is accessible at:
     * http://[address]:[port]/entry-point/service/[user IP]/[User Id]
     * 
     * @param args - in the aforementioned format
     * @throws Exception - if something goes wrong
     */
    public static void main(String[] args) throws Exception {
        // Parse the command line parameters.
        InputStream cloudSiteStream = args.length > 0 ? new FileInputStream(args[0]) : 
            Main.class.getResourceAsStream("/cloudsites.properties");
        InputStream configStream = args.length > 1 ? new FileInputStream(args[1]) : 
            Main.class.getResourceAsStream("/config.properties");
        int jettyPort = args.length > 2 ? Integer.parseInt(args[2]) : Configs.DEFAULT_EP_PORT;
        
        // The server to start
        Server jettyServer = new Server(jettyPort);

        try (AutoCloseable serverClosable = jettyServer::destroy;
                EntryPoint ep = EntryPoint.getInstance()) {
            // Configure the entry point
            EntryPoint.getInstance().configure(cloudSiteStream, configStream, RESTCloudSite.FACTORY, new GeoIP2PingERService());

            // Configure the servelet context
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            jettyServer.setHandler(context);
            ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(0);

            // Tells the Jersey Servlet which REST service/class to load.
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                    EntryPointService.class.getCanonicalName());

            // Start the server, and join with its thread
            jettyServer.start();
            jettyServer.join();
        }
    }
}
