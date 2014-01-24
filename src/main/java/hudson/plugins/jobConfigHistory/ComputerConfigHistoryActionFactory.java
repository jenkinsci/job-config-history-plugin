/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.model.Slave;
import hudson.model.TransientComputerActionFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Lucie Votypkova
 */
@Extension
public class ComputerConfigHistoryActionFactory extends TransientComputerActionFactory {

    @Override
    public Collection<? extends Action> createFor(Computer computer) {
        final List<Action> actions = new ArrayList<Action>();
        if (computer.getNode() instanceof Slave) {
            actions.add(new ComputerConfigHistoryAction((Slave) computer.getNode()));
        }
        return actions;
    }
    
    
}
