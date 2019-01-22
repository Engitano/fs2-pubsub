FROM google/cloud-sdk:229.0.0-alpine

LABEL maintainer "Mark Tranter <mark.tranter@engitano.com>"

RUN apk --update add openjdk7-jre
RUN gcloud components install --quiet beta pubsub-emulator
RUN mkdir -p /var/pubsub

VOLUME /var/pubsub

ENTRYPOINT [ "gcloud", "beta", "emulators", "pubsub", "start", "--data-dir=/var/pubsub", "--log-http", "--verbosity=debug", "--user-output-enabled" ]
