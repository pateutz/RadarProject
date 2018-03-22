 YUM_CMD=$(which yum)
  APT_GET_CMD=$(which apt-get)
  OTHER_CMD=$(which <other installer>)

 wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclel
 sudo yum install jdk-9.0.1_linux-x64_bin.rpm
 rm jdk-9.0.1_linux-x64_bin.rpm
 
 if [[ ! -z $YUM_CMD ]]; then
    yum install $YUM_PACKAGE_NAME
 elif [[ ! -z $APT_GET_CMD ]]; then
    apt-get $DEB_PACKAGE_NAME
 elif [[ ! -z $OTHER_CMD ]]; then
    $OTHER_CMD <proper arguments>
 else
    echo "error can't install package $PACKAGE"
    exit 1;
 fi
