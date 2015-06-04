package org.cloudbus.mcweb.admissioncontroller;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * An entry point servelet, which redirects
 * 
 * @author nikolay.grozev
 *
 */
public class PingServelet extends HttpServlet {

    /** Default Serial Version UID. */
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException {
    }

    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    	response.setContentType("text/html");

        // Actual logic goes here.
        PrintWriter out = response.getWriter();
        out.println("x");
    }

    public void destroy() {
        // do nothing.
    }

}
