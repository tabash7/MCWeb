package org.cloudbus.mcweb.admissioncontroller;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.cloudbus.mcweb.DataCentre;
import org.cloudbus.mcweb.ServerFarm;
import org.cloudbus.mcweb.util.Configs;
import org.cloudbus.mcweb.util.Jsons;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.google.common.base.Preconditions;

/**
 * Starts a local admission controller. 
 * 
 * @author nikolay.grozev
 */
public class Main {

    static {
        Logger log = LogManager.getLogManager().getLogger("");
        for (Handler h : log.getHandlers()) {
            h.setLevel(Level.WARNING);
        }
    }

    private static Logger LOG = Logger.getLogger(Main.class.getCanonicalName());
    
    /**
     * Starts a local admission controller. The arguements should be in the form:
     * 
     *      java -jar jar-file.jar [data-centre-json-file] [admission-rule-class] [port]
     * 
     * The resulting server is accessible at:
     * http://[address]:[port]/admission-control/service?userTokens=[token1],[token2],[token3]
     * 
     * @param args
     *            - in the aforementioned format
     * @throws Exception
     *             - if something goes wrong
     */
    public static void main(String[] args) throws Exception {

        // Parse command line arguements
        InputStream dataCentreStream = args.length > 0 ? new FileInputStream(args[0]) : Main.class.getResourceAsStream("/DataCentreConfig.json");
        Class<?> ruleClass = args.length > 1 ? Class.forName(args[1]) : RuleBasedControllerRule.class;
        Preconditions.checkArgument(IAdmissionControllerRule.class.isAssignableFrom(ruleClass), 
                String.format("%s is not an instance of %s", ruleClass.getCanonicalName(), IAdmissionControllerRule.class.getSimpleName()));
        IAdmissionControllerRule rule = (IAdmissionControllerRule) ruleClass.newInstance();
        
        Class<?> resolverClass = args.length > 2 ? Class.forName(args[2]) : TestUserResolver.class;
        Preconditions.checkArgument(IUserResolver.class.isAssignableFrom(resolverClass), 
                String.format("%s is not an instance of %s", resolverClass.getCanonicalName(), IUserResolver.class.getSimpleName()));
        IUserResolver userResolver = (IUserResolver) resolverClass.newInstance();
        
        int jettyPort = args.length > 3 ? Integer.parseInt(args[3]) : Configs.DEFAULT_AC_PORT;
        
        //The data centre for this admission controller
        DataCentre dataCenre = Jsons.fromJson(dataCentreStream, DataCentre.class);
        LOG.warning("Loaded DC definition:" + dataCenre.toString());
        
        // The server to start
        Server jettyServer = new Server(jettyPort);

        try (AutoCloseable serverClosable = jettyServer::destroy;
                AdmissionController controller = AdmissionController.getInstance()) {
            
            // Config the admission controller
            AdmissionController.getInstance().configure(dataCenre, rule, userResolver, ServerFarm.DUMMY_FARM);
            
            // Set up the server context
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            jettyServer.setHandler(context);
            ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
            jerseyServlet.setInitOrder(0);
            ServletHolder webServlet = context.addServlet(PingServelet.class, "/ping/*");
            webServlet.setInitOrder(0);

            // Tells the Jersey Servlet which REST service/class to load.
            jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                    AdmissionControllerService.class.getCanonicalName());

            // Start the server's thread and wait for it to complete.
            jettyServer.start();
            jettyServer.join();
        }
    }
}
