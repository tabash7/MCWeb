package org.cloudbus.mcweb.experimentclient;

import java.io.FileInputStream;
import java.io.InputStream;


import com.google.common.base.Preconditions;

/**
 * Starts a local admission controller. 
 * 
 * @author nikolay.grozev
 */
public class Main {
    
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
	System.out.println("Hello from client");
    }
}
