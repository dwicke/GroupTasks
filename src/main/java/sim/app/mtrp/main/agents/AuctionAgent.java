package sim.app.mtrp.main.agents;

import sim.app.mtrp.main.*;
import sim.app.mtrp.main.agents.Valuators.Auction;
import sim.util.Bag;

/**
 * Created by drew on 3/11/17.
 */
public class AuctionAgent extends LearningAgent {

    public AuctionAgent(MTRP state, int id) {
        super(state, id);
    }


    @Override
    public Task getAvailableTask() {

        Neighborhood chosenNeighborhood = null;
        double maxUtil = 0.0;
        for (Neighborhood n : state.getNeighborhoods()) {
            Bag tasksToConsider = getAvailableTasksWithinRange(n.getTasks());
            double tasksInRange = (double) tasksToConsider.size();
            double availableBounty = 0.0;
            Task[] arrTasks = (Task[]) tasksToConsider.toArray(new Task[tasksToConsider.size()]);
            for (int i = 0; i < arrTasks.length; i++) {
                availableBounty += arrTasks[i].getBounty();
            }

            if (tasksInRange > 0) {
                double numAgentsInNeighborhood  = 1; // use the agent location preditor to figure this out.
                double confidence = (1.0 - (n.getTasks().length / n.getMaxTasks())) + 1.0;
                double totalBounty = n.getBounty(); //+ availableBounty * (1 / (1 + numAgentsInNeighborhood));
                double util =  ( confidence *  (-getCost(n) + totalBounty+ (getNumTimeStepsFromLocation(n.getLocation()) ) * state.getIncrement())) /  (getNumTimeStepsFromLocation(n.getLocation()));
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

        Bag nonCommitedTasks = new Bag();
        for(Object o : tasksWithinRange) {
            Task t = (Task)o;
            if (t.getCommittedAgents().size() == 0) {
                nonCommitedTasks.add(t);
            }
        }

        return getAvailableTask(nonCommitedTasks);







        //return getAvailableTask(getNonCommittedTasks());
        //return getAvailableTask(getTasksWithinRange(state.getBondsman().getNewTasks()));
    }

    @Override
    public Task getAvailableTask(Bag nonCommitedTasks) {

        if (curJob == null) {
            //state.printlnSynchronized("Agent " + getId() + " getting task = " + getBestCounter++);

            // so here do the auction!

            // basically
            // merge the valuations from all agents
            // find max
            // if index of max / numTasks == agentID then we have found the task we want
            // then its just a matter of index of max % numTasks

            // if it is not this agent's id then note the agent and the task id
            // remove that agent's valuations by setting all of its valuations to -Max
            // then set all of the agent valuations for that task to -MAX

            // DO i look at all of the task? or just those in the range of me?  i think just those in range.
            //Bag nonCommitedTasks = getNonCommittedTasks();
            Task[] availableTasks = (Task[]) nonCommitedTasks.toArray(new Task[nonCommitedTasks.size()]);

            if (availableTasks.length > 0) {
                double[][] valuations = new double[state.agents.length][availableTasks.length];
                //System.err.println("Num avail tasks = " + availableTasks.length);
                // for each agent get their valuation
                for (int i = 0; i < state.agents.length; i++) {
                    valuations[i] = ((AuctionAgent) state.agents[i]).getEvaluations(availableTasks);// agent id corresponds to agent's index.
                }

                //Task[] availableTasks = state.bondsman.getAvailableTasks();
                Auction a = new Auction(state);
                int index = a.runAuction(availableTasks.length, valuations, this.getId());
                if (index == -1) {
                    state.printlnSynchronized("Agent " + id + " index for task returned was -1 we have a problem...");
                    return null;
                }
                return availableTasks[index];

            } else {
                // no tasks to choose from.
                return null;
            }
        }
        return curJob.getTask();
    }




    @Override
    public void commitTask(Task t) {
        t.amCommitted(this);
    }

    @Override
    public void decommitTask() {
        if (curJob != null)
            curJob.getTask().decommit(this);
        super.decommitTask();

    }


    public double getUtility(Task t) {

        // this seems to work the best!!!!!!!!! for some reason... got to figure this out.
        //double util =  ( (t.getBounty()+ getNumTimeStepsFromLocation(t.getLocation()) - getCost(t))) /  (getNumTimeStepsFromLocation(t.getLocation()) );
        //double util =   (t.getBounty()+ (getNumTimeStepsFromLocation(t.getLocation()) + tTable.getQValue(t.getJob().getJobType(), 0)) * state.getIncrement() - getCost(t)) /  (getNumTimeStepsFromLocation(t.getLocation()) + tTable.getQValue(t.getJob().getJobType(), 0));
        double util =   (-getCost(t) + t.getBounty()+ (getNumTimeStepsFromLocation(t.getLocation()) + tTable.getQValue(t.getJob().getJobType(), 0)) * state.getIncrement() - 0) /  (getNumTimeStepsFromLocation(t.getLocation()) + tTable.getQValue(t.getJob().getJobType(), 0));

        return util;
    }

    public double getCost(Neighborhood t) {
        Depo closestDepo = getClosestDepo(t.getLocation());
        if (closestDepo == null) {
            return Double.POSITIVE_INFINITY; // can't get to a depo from here! this is the case if i'm being asked what I'd bid for it.
        }
        return getNumTimeStepsFromLocation(t.getLocation()) * closestDepo.getFuelCost();
    }

    public double getCost(Task t) {
        Depo closestDepo = getClosestDepo(t.getLocation());
        if (closestDepo == null) {
            return Double.POSITIVE_INFINITY; // can't get to a depo from here! this is the case if i'm being asked what I'd bid for it.
        }
        return getNumTimeStepsFromLocation(t.getLocation()) * closestDepo.getFuelCost();
    }

    public double[] getEvaluations(Task[] availTasks) {
        double[] valuations = new double[availTasks.length];
        int i = 0;
        for (Task t: availTasks) {
            valuations[i] = getUtility(t);
            i++;
        }
        return valuations;
    }






}
