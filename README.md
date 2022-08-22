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
the proposed ```FaaST``` is chosen. The loop is assumed to have 60 iterations. 
Note, that by changing the field ```ALGRORITHM``` in [ScheduleInvoker](src/main/java/at/ac/uibk/ScheduleInvoker.java) 
to ```Random```, a random scheduler is chosen.

## Installation and Execution

```git clone https://github.com/pgritsch/faast_public.git && cd faast_public```

```gradle clean build ```

```java -jar build/libs/afcl_published-1.0-IEEE.jar```

## Example Workflow - BWA
For BWA see [https://doi.org/10.1093/bioinformatics/btp698](https://doi.org/10.1093/bioinformatics/btp698)

### Input file (full file see [BWAfaast4_afcl.yaml](BWAfaast4_afcl.yaml))

Note that no resource is defined, on where to execute the given function, but just the function type (FT) ```splitType```

#### Yaml Representation

```yaml
parallelBody:
   - section:
       - function:
           name: "split0"
           type: "splitType"
           dataIns:
             - name: "fastaFolder"
               type: "string"
               source: "pSample/fastaFolder"
             ### further inputs omitted for brevity ###
             - name: "chunks"
               type: "number"
               source: "pSample/chunks"
           dataOuts:
             - name: "fasta"
               type: "string"
```
#### Graphical Representation (created with [http://fceditor.dps.uibk.ac.at:8180/#/editor](http://fceditor.dps.uibk.ac.at:8180/#/editor))

![](readme_img/bwa.png)

### Output file

#### Yaml Representation

Note that after scheduling the ```resource``` property is filled with an IBM cloud function. 

Additionally, the name has an appended subscript. 
This is because the inner parallel is splitted again to allow for each segment to be executed on different resources.
(see the graphical depiction with 4 separate branches, where each of the branches runs on a different resource configuration).

```yaml
parallelBody:
  - section:
    - function:
        name: "split0_0"
        type: "splitType"
        dataIns:
        - name: "fastaFolder"
          type: "string"
          source: "pSample_0/fastaFolder"
        ### further inputs omitted for brevity ###
        - name: "chunks"
          type: "number"
          source: "pSample_0/chunks"
        dataOuts:
        - name: "fasta"
          type: "string"
        properties:
        - name: "resource"
          value: "https://eu-gb.functions.appdomain.cloud/api/v1/web/office%40dps.uibk.ac.at_dev/bwa/split.json"
```

#### Graphical Representation 

![](readme_img/workflow_scheduled.png)