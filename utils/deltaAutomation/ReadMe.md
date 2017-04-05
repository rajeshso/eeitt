This application takes a spreadsheet and returns a text file containing the data from the ETMP delta ready to be loaded through the endpoint.

1. To use this application you need a few things:

   1. An input folder in which to place the .xlsx or .xls
   files to be processed.
        
   2. An output folder where ether output records will be kept.
        
   3. An output folder to store the files containing the records which could not be parsed.
        
   4. The jar file which the script requires.
        
        
2. The arguments that the script take are in the following format:

   ./DeltaAutomation "full/path/to/inputFolder" "full/path/to/outputfolder" "full/path/to/badFolder" "fileName.xlsx"

   The files produced by this will be of the original name with a time stamp appended to them.

