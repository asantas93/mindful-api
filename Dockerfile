FROM nightscape/docker-sbt:latest

RUN mkdir /usr/local/mm

WORKDIR /usr/local/mm

ADD build.sbt build.sbt
ADD project project
ADD app app
ADD conf conf
ADD .service_key.json .service_key.json

RUN sbt dist
RUN unzip target/universal/mm-api-1.0-SNAPSHOT.zip

ADD KeyStore.jks KeyStore.jks

EXPOSE 9443

ENTRYPOINT /usr/local/mm/mm-api-1.0-SNAPSHOT/bin/mm-api \
  -Dconfig.file=/usr/local/mm/conf/application.conf \
  -Dhttps.port=9443 \
  -Dhttp.port=disabled \
  -Dplay.server.https.keyStore.path=/usr/local/mm/KeyStore.jks \
  -Dplay.server.https.keyStore.password=$KEYSTORE_PW \
  -Djdk.tls.ephemeralDHKeySize=2048 \
  -Djdk.tls.rejectClientInitiatedRenegotiation=true
