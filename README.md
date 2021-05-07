# CkTailv2

Implementation of the model learning approach CkTail (second version)

CkTailv2 aims at recovering models from event logs for communicating systems. It generates behavioural models (IOLTSs) for every component along with dependency graphs lustrating directional dependencies.

## Usage
You can compile the program by running the script **build.sh**.

Once compiled, you can run the method with the script **CkTail.sh**:
  ```
  CkTail.sh -i <input> -r <regex> -o <output>
  ```
  **\<input\>** : your input log.

  **\<regex\>** : the file that contains the regex that cover the actions in the log.
  
  Models are generated in the **\<output\>** folder.

Other options: 

-t   shows the duration of each step of the program.

It is an eclipse project, you can import it directly with eclipse if you want to modify the code.

## Method
CkTail infers a system of IOLTSs, where each IOLTS represents a component communicating in the system. It takes as input an actions log of the system, and a regex file containing regular expression that match the actions in the log. It also extracts dependencies between the different components of the system and express the m in DAGs (Directed Acyclic Graphs). 

### Prerequisite
Each action of the log has to contains a timestamp, following the date format given in the regex file with the line:

**-d \<dateFormat\>** 

The different groups of the regex are the following:

*date* : the timestamp of the event

*label* : the label of the event

*parami* : a parameter of the event, with *i* incrementing from 1


The actions has to contain as parameter an identifier of the component that has sent the message (the source, denoted "Host=\<id\>" in the log), and an identifier of the component that receive the message (the destination, denoted "Dest=\<id\>" in the log). 

The actions log is composed of *Requests* and *Responses*. Each *Request* has at least one associated *Response* next in the log. (Currently, *Responses* are identified in the log by containing the word "response" in the label or the parameters) 

Internal actions are events where the source and the destination are equals.

### Overview
The implementation works in two parts. 
The first part, Split, aims to analyse the log in order to extract the dependencies between the components, and separates the log into many traces that capture the sessions. 
The traces generated are generated in the folder  **\<output\>/splitres/traces**, and the dependency graph

The second part, model generation, infers models of the different components, using the traces generated in the previous step. The input action are marked with a "?", and the output with a "!" at the begining of the label of the transition. Internal actions have no mark. 
The models of the components are generated in the folder **\<output\>/dot**.


## Contents
**CkTailV2/src/** contains the implementation of the method. (eclipse project)

**CkTailV2/examples/** contains examples of action log and regex.

**CkTailV2/RESULTS/** The folder that will contains the results for the cases in **CkTail/log**. The results from the case in **CkTailV2/examples/** are already inside.


## TODO
 - [ ] allow the user to define how to identify *Requests* and *Responses* 
 - [ ] allow the user to define the difference in the ontime (currently 2.0 sec)
 - [ ] fix the genration of the pdf from the dot
