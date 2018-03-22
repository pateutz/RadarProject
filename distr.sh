  GNU nano 2.5.3              File: distr.sh                                    

# i tested only on ubunt bcs i dont have centos right now but i will improve so$
YUM_CMD=$(yum)
APT_GET_CMD=$(apt-get)

 if [[ ! -z $YUM_CMD ]]; then
    sudo yum update 
  #  sudo yum install jdk9 # (i dont have centos for testing but i will improve$
  
 elif [[ ! -z $APT_GET_CMD ]]; then
    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt install oracle-java9-installer
    sudo apt install oracle-java9-set-default
 else
    echo "error can't install package"
  
