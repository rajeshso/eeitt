This application takes a password protected spreadsheet and returns a text file containing the data from the ETMP delta ready to be loaded through the endpoint.

1. In order to use this tool you will need to change your Java Cryptography Extension (JCE) from the standard version to the unlimited strength version please see the below link:

https://cwiki.apache.org/confluence/display/STONEHENGE/Installing+Java+Cryptography+Extension+(JCE)+Unlimited+Strength+Jurisdiction+Policy+Files+6

If you are a windows or a mac user you will need to replace these files after every java update. If you area Linux user you can set up your JCE unlimited to be updated along with Java.

JCE unlimited installation Ubuntu change java version as necessary:
sudo add-apt-repository ppa:webupd8team/java
sudo apt update
sudo apt install oracle-java8-unlimited-jce-policy

2. Ensure you have the delta-automation-assembly-1.0.jar, JRE8+ and Scala v2.12.1+ are in an accessible classpath

3. To use this service please run the DeltaAutomationScript with the arguments for the script. The first argument needs to be quoted due to spaces. An example of running the script would be.

./DeltaAutomationScript.sh INPUTFILELOCATION OUTPUTFILELOCATION BADFILELOCATION INPUTFILENAME PASSWORD

For example, the script can be run as follows
./DeltaAutomationScript.sh "/home/Downloads/delta/input" "/home/Downloads/delta/output" "/home/Downloads/delta/bad" "New Agent Details 06.03.2017 (1).xlsx" ABCDE9ASSW0RD

The output of the file would like like "MonMar2010:39:48GMT2017New Agent Details 06.03.2017 (1).xlsx.txt" in /home/Downloads/delta/output

The output of the script, the transformed file would be generated in the specified output location. If the Input file is not in the expected format
or if the input has records that don't belong to the same user, those content would not be part of the output. Such content would be moved to a bad file location.
Please check with the eeitt development team on the expected input format of the file and the transformed output format of the file.

PASSWORD refers to the password for the file. When this script is executed, the output file will have the current date and time appended to it.