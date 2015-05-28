#!/bin/bash 
if [ ! $# == 1 ]; then
    echo "ERROR:   usage    create_user_cred NAME_OF_USER"
    exit
fi
if [ -e certs/$1.crt ]; then
    echo "ERROR:   User already exists"
    exit
fi

cd /etc/pki
openssl req -new -config conf/client-new.conf -out certs/$1.csr \
-keyout certs/$1.key
openssl ca -config conf/tls-ca.conf -in certs/$1.csr \
-out certs/$1.crt -policy extern_pol -extensions client_ext
openssl pkcs12 -export -caname "ACME TLS CA" -caname "ACME Root CA" \
-inkey certs/$1.key -in certs/$1.crt -certfile ca/tls-ca-chain.pem \
-out certs/$1.p12
cp certs/$1.p12 export_PKCS12/$1.p12
openssl rsa -in certs/$1.key -pubout -outform PEM -out user_pub_keys/$1_pub.pem
# inserting into in MySQL database for API
echo "Writing to MySQL..."
NAME=$( openssl x509 -in certs/$1.crt -text | sed -En 's/.*Subject.*CN=//p' )
PUB_KEY=$( cat user_pub_keys/$1_pub.pem )
if [ "$NAME" ] && [ "$PUB_KEY" ] ; then
        echo "INSERT INTO users (name, pub_key) VALUES ('"$NAME"', '"$PUB_KEY"')" | mysql -h "localhost" -u "root" "-pbnss2015" "api"
else
        echo "Error detected, aborting mysql insertion"
fi
echo ""
echo "-------------------------------------------"
echo "User credentials" $1".p12 for export in /etc/pki/export_PKCS12/"

echo "Starting xPKI python script"
python2.7 /etc/pki/xPKI/xpki.py $1
