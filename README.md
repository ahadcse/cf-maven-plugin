mvn clean install -Daws.accessKeyId=aws_key_id -Daws.secretKey=aws_secret_key -DawsAccountId=aws_account_id -DawsRegion=aws_region

if only mvn clean install command is given then it will look for credential in local ~/.aws/ directory