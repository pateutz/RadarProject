                                
# i tested only on ubuntu bcs i dont have centos right now but i will improve soon
YUM_CMD=$(yum)
APT_GET_CMD=$(apt-get)

 if [[ ! -z $YUM_CMD ]]; then
  #  sudo yum update # (testing after centos will be done xD) 
  #  sudo yum install jdk9 # (testing after centos will be done xD)
  
 elif [[ ! -z $APT_GET_CMD ]]; then
    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt install oracle-java9-installer
    sudo apt install oracle-java9-set-default
 else
    echo "error can't install package"
  
