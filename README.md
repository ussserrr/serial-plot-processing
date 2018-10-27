## Description
![screenshot](/screenshots/ProcessingSerialRealTimePlot-1.png)

[Processing](https://processing.org) – free open-source Java-based framework and IDE for easy and rapid prototyping of some sort of graphical applications.

This application retrieves numerical data (multiple channels) from a serial port (MCU's UART for example) and plots it in a real-time mode. The code assumes raw binary integers that are consequentially transmitting one after another in an endless loop. With a lack of any kind of synchronization, situations of wrong start byte can [rarely] occur.

Two versions are available:
 - **pde** – Processing3-compatible sketch to run inside of official IDE. Note it can also be exported to completely stand-alone bundle that you can afterwards execute on almost any PC and OS
 - **Java** – IntelliJ IDEA project that illustrates the ability to use the Processing framework outside of the official IDE and can be compile as a normal Java8 package


## Configuration
Parameters to edit:
 - serial connection (port, speed, etc.)
 - number of channels
 - integer size (in bytes)

In case you decided to use Processing outside of the IDE you need to obtain necessary libraries (Grafica, ControlP5) from the Processing IDE and import them into your Java project to be able to successfully compile and execute the program.


## Notes
Processing' loop mechanism (`draw()` function) is running in approximately 30-60 fps so some simple algorithm to delimit data retreiving and rendering is applied.
