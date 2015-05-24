package org.cloudbus.mcweb.entrypoint;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * An entry point servelet, which redirects
 * 
 * @author nikolay.grozev
 *
 */
public class EntryPointRedirectServelet extends HttpServlet {

    /** Default Serial Version UID. */
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException {
    }

    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        // Get the IP address of the source
        String sourceIP = getClientIpAddr(request);
        String userToken = request.getParameter("UserToken");
        
        // Do the algorithm
        EPUserRequest req = new EPUserRequest(sourceIP, userToken);
        EntryPoint.getInstance().request(req);
        CloudSite cs = req.selectCloudSite();
        String redirectAddress = cs == null ? null : cs.getLoadBalancerAddress();

        if(redirectAddress != null) {
            // Redirect to the result
            response.sendRedirect(redirectAddress);
        }
    }

    public void destroy() {
        // do nothing.
    }

    private static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
