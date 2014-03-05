DogPopulationService
====================

* A Service using Neo4J to build a graph with relationships between dogs.
* Provides REST endpoints for 
** dog pedigree
** breed dashboards
** data inconsistencies

REST API
--------
All results are returned in JSON format with the header: "Content-Type: application/json"  

| Resource        | Action | Result           | Attributes  |
|:------------- |:------------- |:----- |:------ |
| *Pedigree completeness*  
http://dogid.nkk.no/dogpopulation/graph/pedigreecompleteness?generations=6&breed=Rottweiler&minYear=1999&maxYear=2001 | get  | Pedigree Completeness for a selection of dogs in given breed and registration year | generations: number of generations incl. the dog itself  
breed: Case sensitive breed title  
minYear: Min year of registration  
maxYear: Max year of registration |
 
* Inbreeding  http://dogid.nkk.no/dogpopulation/graph/inbreeding?generations=6&breed=Rottweiler&minYear=1999&maxYear=2001
  Inbreeding coefficients are measured in percentage-of-inbreeding. The "frequency" property counts the number of dogs within ranges of inbreeding.
  i.e. frequncy[0] are all dogs with 0% inbreeding, frequency[1] are dogs in range (0,1)%, frequency[2] in range [1,2)%, frequency[3] in range [2,3)%, etc.
* Force import, pointers  http://dogid.nkk.no/dogpopulation/graph/breed/import/Pointer
* HDIndex
  http://dogid.nkk.no/dogpopulation/hdindex/Dalmatiner20140227/data?breed=Dalmatiner&breed=dalmatiner
  http://dogid.nkk.no/dogpopulation/hdindex/Dalmatiner20140227/pedigree?breed=Dalmatiner&breed=dalmatiner
  http://dogid.nkk.no/dogpopulation/hdindex/Dalmatiner20140227/uuidmapping?breed=Dalmatiner&breed=dalmatiner
  The very first time one of the above URLs with path-parameter "Dalmatiner20140227" is used, the query-parameter "breed"
  must be set to one or more breed names. Consequtive access to any of the above URLs will ignore any query parameters,
  and simply return a cached dataset.
* Litter-statistics  http://dogid.nkk.no/dogpopulation/graph/litter?breed=Rottweiler&minYear=1999&maxYear=2001
* Data-Inconsistencies
** http://dogid.nkk.no/dogpopulation/graph/inconsistencies/gender/all?skip=0&limit=10
   Lists all uuids of dogs that have some sort of gender inconsistency
** http://dogid.nkk.no/dogpopulation/graph/inconsistencies/gender/654cecf2-2eb1-4bc0-9d93-5046ed2f82ec
   Get details related to gender inconsistencies of dog with uuid 654cecf2-2eb1-4bc0-9d93-5046ed2f82ec
** http://dogid.nkk.no/dogpopulation/graph/inconsistencies/breed/all?skip=0&limit=10
   Lists uuid of all dogs that have a different breed from both its parents
** http://dogid.nkk.no/dogpopulation/graph/inconsistencies/breed/0426517e-3833-4a29-a620-075ebb1b8b68
   Get details related to breed inconsistencies of dog with uuid 0426517e-3833-4a29-a620-075ebb1b8b68
** http://dogid.nkk.no/dogpopulation/graph/inconsistencies/circularancestry/37c2f6ac-ef93-49e8-a4df-1017df1161ea
   Get parent-chain that form a circle in the ancestry of dog with uuid 37c2f6ac-ef93-49e8-a4df-1017df1161ea
