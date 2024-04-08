## Particle Simulator

This project is a simple particle simulator implemented in Java, to simulate the behavior of particles in a 2D environment. The simulator provides a graphical user interface (GUI) for visualization and interaction with the particles. It features a developer mode where the user can add particles and an explorer mode where the user may navigate a sprite through a zoomed-in version of the canvas. It allows for multiplayer with the host having the Developer Mode UI and the client having the Explorer Mode UI.

### Minimum Java Version

This project requires at least Java 18 to run. Ensure you have Java 18 or a higher version installed on your system before running the program.

### How to Run

#### Option 1: Double Click

1. **Obtain the Jar File:**
   - If not yet provided, download the ff. files from the [releases](https://github.com/jerichosy/STDISCM-Particle-Simulator-PS3/releases/tag/ps3).
      - Server_STDISCM-PS3.jar
      - Client_STDISCM-PS3.jar

2. **Double Click the Jar File:**
   - Locate the downloaded JAR files in your file explorer.
   - Double-click each JAR file to run the program.

#### Option 2: Command Line

1. **Obtain the Jar File:**
   - If not yet provided, download the ff. files from the [releases](https://github.com/jerichosy/STDISCM-Particle-Simulator-PS3/releases/tag/ps3).
      - Server_STDISCM-PS3.jar
      - Client_STDISCM-PS3.jar

2. **Open multiple Command Prompts (Windows) or Terminals (Mac/Linux):**
   - For each terminal, navigate to the directory where the JAR files are located using the `cd` command.

3. **Run the Program:**
   - For each terminal, type the following command and press Enter:
     ```
     java -jar <filename>.jar
     ```

### Interacting with the Simulator

- Once the program is running, you can interact with the simulator through the GUI.
- In developer mode (host/server JAR), use the control panel to add different types of particles (or clear them) and adjust their properties in the environment.
- In explorer mode (client JAR), use the keyboard arrow keys to move the sprite around the zoomed-in canvas.
- Explore different configurations and observe the behavior of particles in the simulated environment.

### Features

- Simulate particles with various behaviors, velocities, and angles.
- Real-time visualization of particle movement and collisions.
- Developer mode control panel for easy manipulation of particle properties and simulation parameters.
- Explorer mode to navigate a zoomed-in version of the canvas Agar.io-style 
- FPS counter to monitor the frame rate of the simulation.

