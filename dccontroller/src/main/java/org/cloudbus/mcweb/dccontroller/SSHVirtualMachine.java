package org.cloudbus.mcweb.dccontroller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;

import org.cloudbus.mcweb.VMType;
import org.cloudbus.mcweb.VirtualMachine;

import com.google.common.base.Preconditions;

/**
 * A VM, which extracts utilisations through ssh.
 * 
 * @author nikolay.grozev
 */
public class SSHVirtualMachine extends VirtualMachine {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(SSHVirtualMachine.class.getCanonicalName());

    /** Username in the remote system. */
    private final String userName;
    /** Pem file for accessing the remote instance. May be null. */
    private final String pemFile;
    /** Password for accessing the remote instance. */
    private final char[] password;

    private SSHClient client;
    private PKCS8KeyFile keyFile;

    /**
     * Constr.
     * 
     * @param address
     *            - see superclass.
     * @param type
     *            - see superclass.
     * @param userName
     *            - the SSH user name. Must not be null.
     * @param pemFile
     *            - the path to the pem file. Must not be null. Must be a valid
     *            file
     */
    public SSHVirtualMachine(final String address, final VMType type, final String userName, final String pemFile) {
        super(address, type);

        Preconditions.checkNotNull(userName);
        Preconditions.checkNotNull(pemFile);
        Preconditions.checkArgument(Files.exists(Paths.get(pemFile)));

        this.userName = userName;
        this.pemFile = pemFile;
        this.password = null;

        initClient();
    }
    
    /**
     * Constr.
     * 
     * @param address
     *            - see superclass.
     * @param type
     *            - see superclass.
     * @param userName
     *            - the SSH user name. Must not be null.
     * @param pemFile
     *            - the path to the pem file. Must not be null. Must be a valid
     *            file
     */
    public SSHVirtualMachine(final String address, final VMType type, final String userName, final char[] password) {
        super(address, type);

        Preconditions.checkNotNull(userName);
        Preconditions.checkNotNull(password);

        this.userName = userName;
        this.pemFile = null;
        this.password = password;

        initClient();
    }

    private void initClient() {
        try {
            client = new SSHClient();
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect(getAddress());

            if (pemFile != null) {
                keyFile = new PKCS8KeyFile();
                keyFile.init(new File(pemFile));
                client.authPublickey(userName, keyFile);
            } else if (password != null) {
                client.authPassword(userName, password);
            }
            
            final Session session = client.startSession();
            final Command cmd = session.exec("whoami");

            String response = net.schmizz.sshj.common.IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(10, TimeUnit.SECONDS);
            
            System.out.println(response);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error connecting to host", e);
            throw new IllegalStateException(e);
        }
    }
    
    public static void main(String[] args) {
        try (Scanner s = new Scanner(System.in)) {

            System.out.println("Testing password");
            char passwordArray[] = s.nextLine().toCharArray();

            new SSHVirtualMachine("localhost", new VMType("smallish", 0.1, 128, 0.1), "nikolay", passwordArray);
        }
    }
    
}
