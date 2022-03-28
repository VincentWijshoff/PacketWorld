
# General documentation for the PacketWorld Application

This project contains code for the PacketWorld Application, a simple Multi-agent framework to develop and evaluate pickup and delivery problems.

<img src="/res/packetworld.gif" alt="The Packet World" width="350">

[//]: # (![The Packet World]&#40;/res/packetworld.gif&#41;)

## Project structure

The project consists of two main parts: (1) the source code and (2) configuration files.


The source code is located under the `src` directory. In the source code you can find 6 main packages:

- agent: Implementations of different agent behaviors and behavior changes.
- environment: Code related to the environment as well as definitions for worlds in the applications.
- gui: Code related to the Graphical User Interface.
- support: Classes which support the functionality of the application.
- synchronizer: Classes used to realize synchronization in the application.
- util: General purpose classes and functions used within the project.


Configuration files can be found in the `configfiles` directory.
A distinction is made between behaviors and environments. 
A behavior configuration file describes the different behavior states an agent can have, and how an agent can transition between these states.

An environment configuration file describes an environment in which the selected implementation is evaluated. The environment contains information about agents, packets, energy stations, etc.

## Running the code

The project uses Apache Maven as a build framework, and is written in Java version 17 or above. To compile the code, simply run the following command:

`mvn compile`

To run the code we use the maven exec plugin. The plugin can be invoked as follows:

`mvn exec:java`

If desired, you can clear previously built source files with the following command:

`mvn clean`

Lastly, if you want to adhere to the coding style of the project, you can run the following command:

`mvn checkstyle:check`


## Final notes

- To quickly get started, make sure to take a look at the example implementation provided in the configuration file `configfiles/behaviors/wander.txt` and the java source file `src/main/java/agent/behavior/wander/Wander.java`.
- It is not necessary to manually edit environment configuration files. If you would like to test different scenarios than the ones already provided, 
simply choose the _environment editor_ in the main menu of the application to edit or create new environments.
- After (or during) a normal run, you can export the monitored actions of agents in your run together with some metadata about the run. Navigate to the _Actions_ window and click on the _export_ button. For batch runs this is done automatically by specifying the output file before starting the runs.


## Solution description

The agent is able to store the locations of seen walls and destinations in its memory. It uses this knowledge to calculate the best direction to move to. For example, to go towards a previously discovered destination out of view while carrying a packet.

### Behavior
- `MoveTo` makes the agent move to a specified position in the Packet-World. The agent calculates the best move based on the its current position, the steps it can take and its memory to include walls it might encounter.
- `PickupPacket` lets the agent pick up a packet and `ReleasePacket` puts it down again.
- `Wander` is the default behavior, often the result of not knowing where to go next. The agent walks around in random directions.

### BehaviorChange
- `ArrivedOnEmpty` happens when an agent reaches the tile it was moving to, but there is nothing there. Can happen when going for a packet and another agent picks it up before arriving.
- `CanPickup` is a check that makes sure agents only pick up packets when they are able to. `CanRelease` is similar.
- `NoGoal` happens when an agent doesn't know where to go next and results in wandering behavior. `SeesGoal` is the opposite.

### Optimizations
The optimizations are currently static booleans which can be toggled in `agent.behavior.basic.Basic`.



