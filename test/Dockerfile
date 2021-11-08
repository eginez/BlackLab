FROM alpine:latest as build
RUN apk add --update alpine-sdk openssl-dev zlib-dev && apk add --no-cache git
RUN cd /tmp && git clone https://github.com/giltene/wrk2.git
RUN cd /tmp/wrk2 && make

FROM node:14-alpine

# make the 'app' folder the current working directory
WORKDIR /app

# Copy package[-lock].json
COPY package*.json ./

# Install wrk2 load testing tool
COPY --from=build /tmp/wrk2/wrk /usr/local/bin
RUN apk add --no-cache libgcc

# Install node modules
# NOTE: if node_modules is on a volume, this might causes
#   issues when e.g. changing node/npm versions (package will seem
#   to be installed already, but it's not the correct version and will not work,
#   so you have to enter the container, remove the node_modules dir and install again)
RUN npm install

# Copy project files
COPY . .

# Run the tests (after sleeping for a few seconds to let the server start up)
CMD [ "/bin/sh", "./run-tests.sh"]
