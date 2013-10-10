FreeEed Search UI
========
Currently need apache-solr-4.0

To build a war file:

mvn war:war

To Enable CAC use:

Create/purchase an SSL certificate for your tomcat server(s).  Store it in a java keystore.  (I named my keystore keystore.jks).  Self signed Certificate instructions at http://tomcat.apache.org/tomcat-6.0-doc/ssl-howto.html.

Create a Java Certificate repository with the CA Certificates for your PKI.  The easiest way is to start with the Certificate repository at $JAVA_HOME/lib/security/cacerts and add your PKI certificates.  For US Military CAC cards
    follow the instructions at http://www.militarycac.com/notes.htm#DOD_Certificates to download the certificates and use the java keytool to import them.  I copied my CA repository to Tomcat's ${catalina.base}/certificates/ so a Java update does not overwrite my imported certificates.  If you do not copy it there, set truststoreFile to your actual path.

Add the following to your Tomcat server.xml:  (Use of two ports is so that it only asks for your CAC when you click CAC login, not every page if you login normally.)

    <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
    maxThreads="150" scheme="https" secure="true"
    clientAuth="false" sslProtocol="TLS"
    keystoreFile="${catalina.base}/certificates/keystore.jks"
    keystorePass="changeit"
    truststoreFile="${catalina.base}/certificates/cacerts"
    truststorePass="changeit"
    />
    
    <Connector port="8444" protocol="HTTP/1.1" SSLEnabled="true"
    maxThreads="150" scheme="https" secure="true"
    clientAuth="want" sslProtocol="TLS"
    keystoreFile="${catalina.base}/certificates/keystore.jks"
    keystorePass="changeit"
    truststoreFile="${catalina.base}/certificates/cacerts"
    truststorePass="changeit"
    />
