# FaaST: Public Demo Version

## System requirements

- JDK 11 (necessary)
- Gradle > 7.4 (tested with 7.4)

## Short description

This repository contains a version of FaaST, which runs without requiring the whole AFCL tech-stack. 
While the full version utilizes a database server, to retrieve metadata information about the available function types (FT),
its correspoding deployments (FD) and important scheduling information like
average round trip time (avgRTT), this version is configured to retrieve this information from 
static files located in [metadata](metadata).

The used setup provides you with an environment which is easy to set up (without requiring a separate DB) and is able to deliver
reproducable results. 

The class [ScheduleInvoker](src/main/java/at/ac/uibk/ScheduleInvoker.java) represents the Main of this Java program
and can be used to further customize input and output files.
By default, the input file is ```BWAfaast4_afcl.yaml```, the output file ```BWAfaast4_cfcl.yaml``` and as Scheduler 
the proposed ```FaaST``` is chosen. 
Note, that by changing the field ```ALGRORITHM``` in [ScheduleInvoker](src/main/java/at/ac/uibk/ScheduleInvoker.java) 
to ```Random```, a random scheduler is chosen.

## Installation and Execution

```git clone https://github.com/pgritsch/faast_public.git && cd faast_public```

```gradle clean build ```

```java -jar build/libs/afcl_published-1.0-IEEE.jar```


