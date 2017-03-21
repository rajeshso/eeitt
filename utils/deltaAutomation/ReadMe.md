This application takes a password protected spreadsheet and returns a text file containing the data from the ETMP delta ready to be loaded through the endpoint.

1. In order to use this tool you will need to change your Java Cryptography Extension (JCE) from the standard version to the unlimited strength version please see the below link:

    https://cwiki.apache.org/confluence/display/STONEHENGE/Installing+Java+Cryptography+Extension+(JCE)+Unlimited+Strength+Jurisdiction+Policy+Files+6

    If you are a windows or a mac user you will need to replace these files after every java update. If you area Linux user you can set up your JCE unlimited to be updated along with Java.

    JCE unlimited installation Ubuntu change java version as necessary:
    sudo add-apt-repository ppa:webupd8team/java
    sudo apt update
    sudo apt install oracle-java8-unlimited-jce-policy

2. To use this application you need a few things:

   1. An input folder in which to place the .xlsx foiles to be processed.
        
   2. An output folder wher ethe output records will be kept.
        
   3. An output flder to store the files containing the records which could not be parsed.
        
   4. The jar file which the script requires.
        
        
3. The arguments that the script take are in the following format: 

   ./DeltaAutomationScript "full/path/to/inputFolder" "full/path/to/outputfolder" "full/path/to/badFolder" "fileName.xlsx" PASSWORD

   The files produced by this will be of the original name with a time stamp appended to them.