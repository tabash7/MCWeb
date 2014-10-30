package org.cloudbus.mcweb.entrypoint;

import java.io.FileInputStream;
import java.io.InputStream;

import org.cloudbus.cloudsim.ex.geolocation.geoip2.GeoIP2PingERService;
import org.cloudbus.mcweb.util.Configs;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {
    public static void main(String[] args) throws Exception {

        InputStream cloudSiteStream = args.length > 0 ? new FileInputStream(args[0]) : 
            Main.class.getResourceAsStream("/cloudsites.properties");
        InputStream configStream = args.length > 1 ? new FileInputStream(args[1]) : 
            Main.class.getResourceAsStream("/config.properties");

        int jettyPort = args.length > 2 ? Integer.parseInt(args[2]) : Configs.DEFAULT_EP_PORT;
        Server jettyServer = new Server(jettyPort);

        try (AutoCloseable serverClosable = ()-> jettyServer.destroy();
                EntryPoint ep = EntryPoint.getInstance()) {
            EntryPoint.getInstance().configure(cloudSiteStream, configStream, RESTCloudSite.FACTORY, new GeoIP2PingERService());

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            jettyServer.setHandler(context);

            ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(0);

            // Tells the Jersey Servlet which REST service/class to load.
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                    EntryPointService.class.getCanonicalName());

            jettyServer.start();
            jettyServer.join();
        }
    }
}
