package sim.app.mtrp.main;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.Double2D;

import java.util.ArrayList;

/**
 * Created by drew on 2/20/17.
 */
public class Neighborhood implements Steppable, BountyTask{
    private static final long serialVersionUID = 1;

    MTRP state;
    int id;

    Double2D meanLocation;
    ArrayList<Task> tasks;

    int totalTime, count, totalBounty, totalNumTasksGenerated, maxTasks;

    double timestepsTilNextTask;

    Task latestTask = null;
    long numberCompleted = 0;
    long timeWaited = 0;
    long totalTimeWaited = 0;

    // there should be a bounty for actually completeing this task
    // the task of finishing all of the tasks within the neighborhood
    // this may require the assistance of other agents.
    // this is the amount paid to each of the agents that have been working on it
    long bounty = 100;



    public Neighborhood(MTRP state, int id) {
        this.state = state;
        this.id = id;

        // first set the mean location for the neighborhood this will always be within the bounds of the simulation size
        meanLocation = new Double2D(state.random.nextDouble(true,true)*state.simWidth, state.random.nextDouble(true,true)*state.simHeight);
        // then generate the initial tasks locations
        tasks = new ArrayList<Task>();
        timestepsTilNextTask = state.timestepsTilNextTask;
        maxTasks = 2;//state.random.nextInt(10) + 1;
    }


    public void step(SimState simState) {


        if (tasks.size() == 0 && timeWaited != 0) {
            numberCompleted++;
            totalTimeWaited += timeWaited;
            timeWaited = 0;
            bounty = totalTimeWaited / numberCompleted;
            // then we change our mean location
            state.neighborhoodPlane.remove(this);
            meanLocation = new Double2D(state.random.nextDouble(true,true)*state.simWidth, state.random.nextDouble(true,true)*state.simHeight);
            while (state.neighborhoodPlane.getNeighborsWithinDistance(getMeanLocation(), state.meanDistBetweenNeighborhoods).size() != 0) {
                meanLocation = new Double2D(state.random.nextDouble(true,true)*state.simWidth, state.random.nextDouble(true,true)*state.simHeight);
            }
            state.neighborhoodPlane.setObjectLocation(this, this.meanLocation);
        }
        bounty++;
        timeWaited++;
        // here we decide if we create a new task
        generateTasks();
    }

    public Double2D getMeanLocation() {
        return meanLocation;
    }

    public void setMeanLocation(Double2D meanLocation) {
        this.meanLocation = meanLocation;
    }

    @Override
    public String toString() {
        return "id = " + id + " mean (" + meanLocation.getX() + ", " + meanLocation.getY() + ")" + " numTasks = " + tasks.size();
    }


    public void generateTasks() {
        if (state.random.nextDouble() < (1.0 / getTimestepsTilNextTask()) && tasks.size() < maxTasks) {
            //if (state.schedule.getSteps() % state.timestepsTilNextTask == 0) {
            makeTask();
            if (tasks.size() < maxTasks) {
                makeTask();
            }
        } else {
            latestTask = null;
        }
//        if (state.hasRandomness) {
//
//        } else {
//            if (state.schedule.getSteps() % state.timestepsTilNextTask == 0) {
//                makeTask();
//            }
//        }

    }

    public Task makeTask() {
        // generate a new task
        // first generate its coordinates using a gausian
//        double x = state.random.nextGaussian() * state.taskLocStdDev + meanLocation.getX();
//        double y = state.random.nextGaussian() * state.taskLocStdDev + meanLocation.getY();

        // generate the x and y coordinates within the bounding area of the neighborhood
        double x = meanLocation.getX() + (state.random.nextDouble(true, true) * state.taskLocLength) - state.taskLocLength / 2.0;
        double y = meanLocation.getY() + (state.random.nextDouble(true, true) * state.taskLocLength) - state.taskLocLength / 2.0;

        // generate them within the view
        //double x = (state.random.nextDouble(true, true) * state.getSimWidth());
        //double y = (state.random.nextDouble(true, true) * state.getSimHeight());



        Task genTask = new Task(this, state, new Double2D(x, y));


        if (count == 0) {
            genTask.setBaseBounty(totalTime);
        } else {
            double baseBounty = (double) totalTime / (double) count;
            //state.printlnSynchronized("base bounty in neighborhood " + id + " is = " + (totalTime / count));
            genTask.setBaseBounty(baseBounty + 15 * state.maxMeanResourcesNeededForType);
            double inc = Math.abs(baseBounty - state.getBondsman().getTotalAverageTime());
            genTask.setBountyRate(inc);
        }

        tasks.add(genTask);
        latestTask = genTask;
        totalNumTasksGenerated++;
        return genTask;
    }

    public void finishedTask(Task task) {
        totalTime += task.timeNotFinished;
        totalBounty += task.getBounty();
        count++;
        tasks.remove(task);
    }

    public int getId() {
        return id;
    }

    public int getTotalNumTasksGenerated() {
        return totalNumTasksGenerated;
    }


    public Task[] getTasksWithNoCommittedAgents() {
        Bag availTasks = new Bag();
        for (int i = 0; i < tasks.size(); i++) {
            if (((Task)tasks.get(i)).getCommittedAgents().isEmpty()) {
                availTasks.add(tasks.get(i));
            }
        }

        return (Task[]) availTasks.toArray(new Task[availTasks.size()]);

    }

    public void setTimestepsTilNextTask(double timestepsTilNextTask) {
        this.timestepsTilNextTask = timestepsTilNextTask;
    }

    public double getTimestepsTilNextTask() {
        return timestepsTilNextTask;
    }

    public Task getLatestTask() {
        return latestTask;
    }

    public double getTotalBounty() {
        double total = 0.0;
        for (Task t : tasks) {
            total += t.getBounty();
        }
        return total;
    }

    public Task[] getTasks() {
        if (tasks.size() == 0) {
            return new Task[0];
        }
        return tasks.toArray(new Task[tasks.size()]);
    }

    public long getNumberCompleted() {
        return numberCompleted;
    }

    @Override
    public Double2D getLocation() {
        return meanLocation;
    }

    @Override
    public double getBounty() {
        return bounty;
    }

    public int getMaxTasks() {
        return maxTasks;
    }
}
