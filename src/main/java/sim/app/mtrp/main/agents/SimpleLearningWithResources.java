package sim.app.mtrp.main.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sim.app.mtrp.main.*;
import sim.app.mtrp.main.agents.Valuators.ResourceLearner;
import sim.app.mtrp.main.util.QTable;

/**
 *
 * This is a very simple method.  Assumes the agent does not have a carry capacity.
 * This agent "learns" by exponential averaging the number of resources for each of the
 * types over a trip. (a trip is defined as the time between visiting a depo with a max being the fuel capacity)
 *
 * Created by drew on 3/21/17.
 */
public class SimpleLearningWithResources extends LearningAgentWithCommunication {


    ResourceLearner resourceLearner;
    public SimpleLearningWithResources(MTRP state, int id) {
        super(state, id);
        resourceLearner = new ResourceLearner(state);

    }


    @Override
    public double getCost(Task t) {
        Depo d = getClosestDepo();
        return resourceLearner.getCost(t, d, getNumTimeStepsFromLocation(t.getLocation()), getNumTimeStepsFromLocation(d.getLocation()), getNumTimeStepsFromLocation(d.getLocation(), t.getLocation()));
    }

    public boolean checkNeedResources(Depo nearestDepo) {
        boolean needFuel =  super.checkNeedResources(nearestDepo);
        boolean needOtherResources = resourceLearner.checkNeedResources(nearestDepo, curJob,getBestTask(getTasksWithinRange(state.getBondsman().getAvailableTasks())));
        state.printlnSynchronized("Need fuel = " + needFuel + " need other resources = " + needOtherResources);
        return needFuel || needOtherResources;
    }

    @Override
    public boolean buySellTaskResources(Depo nearestDepo) {
        double preBounty = bounty;
        bounty =  resourceLearner.buySellTaskResources(nearestDepo, bounty, getPercentageJobTypes(getTasksWithinRangeAsArray(state.bondsman.getAvailableTasks())));
        return preBounty != bounty;// i did something if i have different amount of bounty now.
    }

    @Override
    public void claimWork() {
        amWorking = resourceLearner.claimWork(curJob);
        if (amWorking) {
            amWorking = curJob.claimWork(this);
            state.printlnSynchronized("Am working? = " + amWorking);
        } else {
            state.printlnSynchronized("Am working? = " + amWorking);
        }
    }



    public int[] getMyResources() {
        return resourceLearner.getMyResources();
    }




}
