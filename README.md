DogPopulationService
====================


Some quick pointers to the REST services

* Pedigree Completeness  http://dogid.nkk.no/dogpopulation/graph/pedigreecompleteness?generations=6&breed=Rottweiler&minYear=1999&maxYear=2001
* Inbreeding  http://dogid.nkk.no/dogpopulation/graph/inbreeding?generations=6&breed=Rottweiler&minYear=1999&maxYear=2001
  Inbreeding coefficients are measured in percentage-of-inbreeding. The "frequency" property counts the number of dogs within ranges of inbreeding.
  i.e. frequncy[0] are all dogs with 0% inbreeding, frequency[1] are dogs in range (0,1)%, frequency[2] in range [1,2)%, frequency[3] in range [2,3)%, etc.
* Force import, pointers  http://dogid.nkk.no/dogpopulation/graph/breed/import/Pointer
* HD uttrekk  http://dogid.nkk.no/dogpopulation/graph/breed/Dalmatiner/hddata
