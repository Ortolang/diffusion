package fr.ortolang.diffusion.ssh.command;

import org.apache.sshd.server.Command;


/**
 * Abstract class for all SSH commands allowing to provide command args. This class is tipically used after command line parsing
 * to dissociate command name and args.
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 24 September 2009
 */
public abstract class SSHCommand implements Command, Runnable {
    private String[] args;

    public SSHCommand() {
        this.args = new String[0];
    }

    public SSHCommand(String[] args) {
        this.args = args.clone();
    }

    public String[] getArgs() {
        return args;
    }

    public String getArg(int pos) {
        return args[pos];
    }

}
