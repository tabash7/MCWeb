package org.cloudbus.mcweb;

import java.io.FileInputStream;
import java.io.InputStream;

import org.cloudbus.cloudsim.ex.geolocation.geoip2.GeoIP2PingERService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {
    public static void main(String[] args) throws Exception {

        InputStream cloudSiteStreamLoaded = args.length > 0 ? new FileInputStream(args[0]) : Main.class
                .getResourceAsStream("/cloudsites.properties");
        InputStream configStreamLoaded = args.length > 1 ? new FileInputStream(args[1]) : Main.class
                .getResourceAsStream("/config.properties");

        int jettyPort = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

        try (InputStream cloudSitesStream = cloudSiteStreamLoaded;
                InputStream configStream = configStreamLoaded) {
            EntryPoint.configureInstance(cloudSitesStream, configStream, CloudSite.FACTORY, new GeoIP2PingERService());

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            Server jettyServer = new Server(jettyPort);
            jettyServer.setHandler(context);

            ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(0);

            // Tells the Jersey Servlet which REST service/class to load.
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                    EntryPointService.class.getCanonicalName());

            try {
                jettyServer.start();
                jettyServer.join();
            } finally {
                jettyServer.destroy();
            }
        }
    }
}
