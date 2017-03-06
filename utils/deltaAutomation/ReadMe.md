This application takes a password protected spreadsheet and returns a text file containing the data from the ETMP delta ready to be loaded through the endpoint.

1. In order to use this tool you will need to change your Java Cryptography Extension (JCE) from the standard version to the unlimited strength version please see the below link:

https://cwiki.apache.org/confluence/display/STONEHENGE/Installing+Java+Cryptography+Extension+(JCE)+Unlimited+Strength+Jurisdiction+Policy+Files+6

If you are a windows or a mac user you will need to replace these files after every java update. If you area Linux user you can set up your JCE unlimited to be updated along with Java.

JCE unlimited installation Ubuntu change java version as necessary:
sudo add-apt-repository ppa:webupd8team/java
sudo apt update
sudo apt install oracle-java8-unlimited-jce-policy

2. To use this service please run the DeltaAutomationScript with the arguments for the script. The first argument needs to be quoted due to spaces. An example of running the script would be.

./DeltaAutomationScript "home/Downloads/new agent data.xlsx" password agent 

password refers to the password for the file and agent refers to the name of the output file. When this is run the output file will have the current date and time appended to it.