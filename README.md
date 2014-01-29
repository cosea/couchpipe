What is this?
=============
This is a simple java based server that conntects to multiple CouchDB changes feeds, splits the recieved data into single packages and delivers them to some output http server.
This server is intended to be used on the same host as the target http server.

## Why?
Long running HTTP connections are unusual and some frameworks have problems managing them.
Specifically, [vert.x](http://www.vertx.io) has no method to close a long running HTTP connection but we needed this.

See [this](https://groups.google.com/forum/#!topic/vertx/MUVdf4xg_7w) discussion on the topic.


This server provides a minimalistic way to watch for timeouts on changes feeds and to deliver single HTTP requests to some other processor.

Usage
=====
## Configure
Write a config that looks just like the [example_config](example_config.json).
The timeout parameter is specified in milliseconds.

You can specify as many from/to pipes as you wish.

    [
      {
        "from":"http://some.host/some_couchdb/_changes?feed=continuous&heartbeat=1000",
        "from_user":"user",
        "from_pass":"examplepass",
        "to":"http://localhost:9090/some_server",
        "to_user":null,
        "to_pass":null,
        "timeout": 10000
      }
    ]

## Run
It's as simple as

    java -jar couchpipe.jar config.json

## Build

Use Ant:

    ant create_run_jar

Or directly export from eclipse.

## Requirements
Java 1.7


TODO
====
-[] Add some loger. System.out.printline is no good idea.-

[] Move build to gradle

[] Remove Jackson lib

License
=======
Distributed under modified BSD license. See LICENSE.txt for specifics.

Libraries used
==============
[Jackson](http://jackson.codehaus.org/) [license](http://www.apache.org/licenses/LICENSE-2.0)
[Apache Commons Codec](https://commons.apache.org/proper/commons-codec/) [license](http://www.apache.org/licenses/LICENSE-2.0)
