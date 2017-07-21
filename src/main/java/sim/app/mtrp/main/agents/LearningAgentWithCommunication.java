package sim.app.mtrp.main.agents;

import sim.app.mtrp.main.Depo;
import sim.app.mtrp.main.MTRP;
import sim.app.mtrp.main.Neighborhood;
import sim.app.mtrp.main.Task;
import sim.app.mtrp.main.agents.Valuators.AgentLocationPredictor;
import sim.app.mtrp.main.util.QTable;
import sim.util.Bag;
import sim.util.Double2D;

/**
 * so i just pick the neighborhood and then do signalling since dealing with a single neighborhood
 * the probability of success should be based on how little effort i have to contribute to complete the task
 * so basically that is how i can then motivate the agents to move to other neighborhoods.  this is because
 * i can't really base it on the number of agents within the neighborhood since that will get a lot of ties.
 *
 * well i can't base it on the number of tasks i the neighborhood because well then what if the max num tasks
 * in some neighborhood is more than some other neighborhood then well that wouldn't do now would it
 *
 * so should it just be like how we pick tasks!
 *
 * need to learn how many agents are needed to complete the neighborhood.
 *
 * need to signal neighborhoods
 *
 * we are essentially in the single "neighborhood" setting except the tasks in this single neighborhood
 * are actually neighborhoods!  So, we need to signal.
 *
 *
 * learn the number of agents needed to complete a neighborhood and then when picking a neighborhood
 * the success is based on the number of agents that are there divided by the number that you think you need
 *
 * Created by drew on 5/4/17.
 */
public class LearningAgentWithCommunication extends LearningAgentWithJumpship {

    QTable agentSuccess;
    QTable numAgentsNeeded;
    double totalJumpshipDist;
    int numJumpships = 0;

    public LearningAgentWithCommunication(MTRP state, int id) {
        super(state, id);
        agentSuccess = new QTable(state.getNumAgents(), 1, .99, .1, 1.0);
        numAgentsNeeded = new QTable(state.getNumNeighborhoods(), 1, .99, .1, 1.0);
    }

    @Override
    public Task getAvailableTask() {
        //return getAvailableTask(getTasksWithinRange(state.getBondsman().getAvailableTasks()));
        //return getAvailableTask(getTasksWithinRange(state.getBondsman().getNewTasks()));
        Neighborhood chosenNeighborhood = null;
        double maxUtil = 0.0;
        for (Neighborhood n : state.getNeighborhoods()) {
            Bag tasksToConsider = getAvailableTasksWithinRange(n.getTasks());
            double tasksInRange = (double) tasksToConsider.size();
            double availableBounty = 0.0;
            Task[] arrTasks = (Task[]) tasksToConsider.toArray(new Task[tasksToConsider.size()]);
//            for (int i = 0; i < arrTasks.length; i++) {
//                availableBounty += arrTasks[i].getBounty();
//            }

            if (tasksInRange > 0) {
                double numAgentsInNeighborhood  = 1; // use the agent location preditor to figure this out.
                double confidence = 0.0;
                for (int j = 0; j < arrTasks.length; j++) {
                    for (int i = 0; i < state.numAgents; i++) {
                        if (arrTasks[j].getJob().isSignaled(state.getAgents()[i]))
                            numAgentsInNeighborhood += 1;//agentSuccess.getQValue(i, 0);
                    }
                }

                confidence += /*(1.0 - (n.getTasks().length / n.getMaxTasks())) +*/ pTable.getQValue(n.getId(), 0);
                double totalBounty = n.getBounty(); //+ availableBounty * (1 / (1 + numAgentsInNeighborhood));
                double util =  confidence;//( confidence *  (-getCost(n) + totalBounty+ (getNumTimeStepsFromLocation(n.getLocation()) ) * state.getIncrement())) /  (getNumTimeStepsFromLocation(n.getLocation()));
                state.printlnSynchronized("util = " + util);
                if (util > maxUtil) {
                    maxUtil = util;
                    chosenNeighborhood = n;
                }
            }
        }
        Bag tasksWithinRange = new Bag();
        if(curJob != null && curJob.getIsAvailable() == true) {
            tasksWithinRange.add(curJob.getTask());
        }
        if (chosenNeighborhood != null) {
            tasksWithinRange.addAll(getAvailableTasksWithinRange(chosenNeighborhood.getTasks()));
        }

        state.printlnSynchronized("Tasks in range = " + tasksWithinRange.size());

        return getAvailableTask(tasksWithinRange);

    }
    public double getCost(Neighborhood t) {
        // closest depo will never be null because we only consider tasks that are within distance of a depo
        Depo closest = getClosestDepo(t.getLocation());
        if (closest == null) {
            return Double.POSITIVE_INFINITY;
        }
        return getNumTimeStepsFromLocation(t.getLocation()) * closest.getFuelCost();
    }

    @Override
    public void learn(double reward) {
        super.learn(reward);
        if (curJob == null || curJob.getCurWorker() == null) {
            state.printlnSynchronized("CurJob = " + curJob + "reward = " + reward + " am working = " + amWorking);
            if (curJob != null) {
                state.printlnSynchronized("Cur worker is null");
            }
        }
        agentSuccess.update(curJob.getCurWorker().getId(), 0, reward);
        agentSuccess.oneUpdate(oneUpdateGamma);
    }

    @Override
    public double getUtility(Task t) {
        if (t.getJob().isSignaled(this) || t.getJob().noSignals()) {
            return super.getUtility(t);
        } else {
            if (!t.getJob().noSignals()) {
                //state.printlnSynchronized("Time step" + state.schedule.getSteps() + "Job id " + t.getJob().getId() + " is signaled but not by me " + getId());
            }
            double confidence = 1.0;
            for (int i = 0; i < state.numAgents; i++) {
                if (t.getJob().isSignaled(state.getAgents()[i]))
                    confidence *= agentSuccess.getQValue(i, 0);
            }



            double util =  ( confidence *  (-getCost(t) + t.getBounty()+ (getNumTimeStepsFromLocation(t.getLocation()) + tTable.getQValue(t.getJob().getJobType(), 0)) * state.getIncrement() - 0)) /  (getNumTimeStepsFromLocation(t.getLocation()) + tTable.getQValue(t.getJob().getJobType(), 0));
            //double util =  ( confidence *  (t.getBounty()+ getNumTimeStepsFromLocation(t.getLocation()) - getCost(t))) /  (getNumTimeStepsFromLocation(t.getLocation()) );
            return util;
            //return 0; // need to change this.
        }
    }

    @Override
    public boolean travel() {
        boolean hasTraveled = super.travel();
        double signalDist = 0;//state.getThresholdToSignal();
        if (numJumpships > 0) {
            signalDist = totalJumpshipDist / numJumpships;
            // state.printlnSynchronized(" agent id = " + id + " signal dist = " + signalDist);
        }

        if (hasTraveled == true && amWorking == false && curJob != null && curJob.getTask().getLocation().distance(this.curLocation) <= signalDist) {
            curJob.signal(this);
        }
        return hasTraveled;
    }

    @Override
    public Task handleJumpship(Task bestT) {
       // if (curJob.isSignaled(this)) {
        totalJumpshipDist += getNumTimeStepsFromLocation(curJob.getTask().getLocation(), curLocation);
        numJumpships++;

        curJob.unsignal(this);

        return super.handleJumpship(bestT);
    }
}
