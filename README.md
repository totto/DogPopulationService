DogPopulationService
====================

* A Service using Neo4J to build a graph with relationships between dogs.
* Provides REST endpoints for 
   * Importing dogs to build the graph
   * Dog pedigree
   * Breed dashboard data (Inbreeding, pedigree completeness and litter size)
   * HD Index data file generation to run with Per Madsen HD Index Algorithm
   * Data inconsistencies

Memory/Storage/Persistence
-------
Using Neo4J and bla bla bla
Todo Kim: Fill in the blanks

Thread limit in Linux
-------
During update and import of breeds, the application will need to allocate a lot of threads (usually not more than 2000 concurrent threads).
It's important to ensure that the thread limit per user and process is set high enough, otherwise the JVM will die with an OutOfMemoryError while trying to allocate a new thread.

This is the configuration set in production AWS to ensure stable operation of application

```
$ cat /etc/security/limits.conf
* soft nofile 20000
* hard nofile 20000
```

```
$ cat /etc/security/limits.d/90-nproc.conf
*          soft    nproc     21024
root       soft    nproc     unlimited
```

REST API
--------
All results are returned in JSON format with the header: "Content-Type: application/json"  

### Import
| Resource        | Action | Result           | Attributes  |
|:------------- |:------------- |:----- |:------ |
| **Import of specified breed to graph**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/breed/import/Pointer| get |  Start an import, reload too see nr of tasks and status. | **breed**: Case sensitive breed name |
| **Mark all breeds for auto-import to graph**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/breed/importall| get |  Mark all breed-synonym nodes with updatedTo property causing auto-update. | None |
| **(Re)import a specific dog (with pedigree) to graph**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/import/dog/ff433553-b14a-4f9d-9408-c10addbefac4 | get |  Start an import, reload too see nr of tasks and status. | **breed**: Case sensitive breed name |
| **Status of all imports**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/breed/import| get | See status of all ongoing and finished imports | None |
| **Status of thread-pools**<br/>http://dogpopulation.nkk.no/dogpopulation/concurrent/executor/status| get | See status of all thread-pools | None |

### Dog pedigree
| Resource        | Action | Result           | Attributes  |
|:------------- |:------------- |:----- |:------ |
| **Pedigree**<br/>http://dogpopulation.nkk.no/dogpopulation/pedigree/ff433553-b14a-4f9d-9408-c10addbefac4| get | Get pedigree of given dog. This end point auto imports this dog to graph if the breed is not imported before. | **uuid**: The global unique id of this dog |
| **Ficticious Pedigree**<br/>http://dogpopulation.nkk.no/dogpopulation/pedigree/fictitious?father=ff433553-b14a-4f9d-9408-c10addbefac4&mother=ed3a4fd6-1814-4668-ad3d-faa39418a273| get | Get pedigree of the ficticious offspring of the given dog. This end point auto imports the mother and father to graph if necessary. The expected inbreeding coefficients are computed. | **father**: The global unique id of the father <br/>**mother**: The global unique id of the mother |

### Breed dashboard data
| Resource        | Action | Result           | Attributes  |
|:------------- |:------------- |:----- |:------ |
| **Pedigree completeness per breed**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/pedigreecompleteness?generations=6&breed=Rottweiler&minYear=1999&maxYear=2001 | get  | Pedigree Completeness for a selection of dogs in given breed and registration year | **generations**: number of generations incl. the dog itself<br/>**breed**: Case sensitive breed name, can be repeated to cover multiple breeds<br/>**minYear:** Min year of registration<br/>**maxYear**: Max year of registration |
| **Inbreeding per breed**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/inbreeding?generations=6&breed=Rottweiler&minYear=1999&maxYear=2001 | get | Inbreeding coefficients are measured in percentage-of-inbreeding. The "frequency" property counts the number of dogs within ranges of inbreeding. i.e. frequency[0] are all dogs with 0% inbreeding, frequency[1] are dogs in range (0,1)%, frequency[2] in range [1,2)%, frequency[3] in range [2,3)%, etc.| Same as above |
| **Litter-statistics per breed (numbers not quality assured yet!)**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/litter?breed=Rottweiler&minYear=1999&maxYear=2001 | get | Get litter statistics for given breed | Same as above |
| **HDDiagnose statistics by breed and birthyear**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/hdstatistics/bornyear?breed=Dobermann&minYear=2012&maxYear=2012 | get | Get HD-XRay statistics for given breed | Same as above |
| **HDDiagnose statistics by breed and XRay-year**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/hdstatistics/xrayyear?breed=Dobermann&minYear=2012&maxYear=2012 | get | Get HD-XRay statistics for given breed | Same as above |

### HD Index data
See https://wiki.cantara.no/display/NKKFS/HD+indeks for more info. 
The very first time one of the HDIndex with path-parameter "Dalmatiner20140227" is used, the query-parameter "breed" must be set to one or more breed names. Consequtive access to any of the above URLs will ignore any query parameters, and simply return a cached dataset.
  
| Resource        | Action | Result           | Attributes  |
|:------------- |:------------- |:----- |:------ |
| **HDIndex data file**<br/>http://dogpopulation.nkk.no/dogpopulation/hdindex/Dalmatiner20140227/data?breed=Dalmatiner&breed=dalmatiner | get | Get data file for HDinxex calculation | **path**: (ie. Dalmatiner20140227) locates files. Use Breed and date or some other human readable identifier.<br/>**breed**: Case sensitive breed name, can be repeated to cover multiple breeds. | 
| **HDIndex pedigree file**<br/>http://dogpopulation.nkk.no/dogpopulation/hdindex/Dalmatiner20140227/pedigree | get | Get pedigree file | **path**: Same as above<br/>**breed** (optional as long as same folder as above): Same as above |
| **HDIndex UUID mapping file**<br/>http://dogpopulation.nkk.no/dogpopulation/hdindex/Dalmatiner20140227/uuidmapping | get | Get uuid mapping file used to read index results back to graph (not yet implemented) | Same as above |

### Data-Inconsistencies
See https://wiki.cantara.no/display/NKKFS/Datafeil+som+oppdages+og+rettes+i+hundenavet for more info

| Resource        | Action | Result           | Attributes  |
|:------------- |:------------- |:----- |:------ |
|**Incorrect or missing gender - list inconsistencies for specific breed**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/inconsistencies/gender/all?breed=Rottweiler&skip=0&limit=10 | get | Lists all uuids of dogs that have some sort of gender inconsistency | **skip**: Used for paging the list<br/>**limit**: used to limit number of results returned at once.|
|**Incorrect or missing gender - unique dog**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/inconsistencies/gender/654cecf2-2eb1-4bc0-9d93-5046ed2f82ec | get | Get details related to gender inconsistencies of dog. | uuid: unique id of dog |
|**Incorrect or missing breed - list all inconsistencies for specific breed**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/inconsistencies/breed/all?breed=Rottweiler&skip=0&limit=10 | get | List all uuids of dogs that have some sort of breed inconsistency | Same as above |
|**Incorrect or missing breed - unique dog**<br/>http://dogpopulation.nkk.no/dogpopulation/graph/inconsistencies/breed/0426517e-3833-4a29-a620-075ebb1b8b68 | get | Get details related to breed inconsistencies of dog. | uuid: unique id of dog |
|**Circular parent chain in pedigree - list for breed set** circle<br/>http://dogpopulation.nkk.no/dogpopulation/graph/inconsistencies/circularancestry/breed?breed=Dobermann&breed=Dalmatiner| get | Get a list of dogs that are part of a circular parent-chain, we will not list more than one dog from each circle. | breed: a list of breeds to perform the search for |
|**Circular parent chain in pedigree - unique** circle<br/>http://dogpopulation.nkk.no/dogpopulation/graph/inconsistencies/circularancestry/445d3c86-272b-4cd0-bf44-8ccc893af070| get | Get parent-chain that form a circle in the ancestry of dog. | uuid: unique id of dog |

Test-server
-----------
N/A

Prod-server
-----------
http://dogpopulation.nkk.no

CI-server
---------
http://ci.nkk.no/jenkins/job/DogPopulationService/