sbt-web
=======

[![Build Status](https://api.travis-ci.org/sbt/sbt-web.png?branch=master)](https://travis-ci.org/sbt/sbt-web)

This project provides the building blocks for web oriented sbt plugins by bringing together the following concerns:

* File directory layout conventions for resources intended to be served by a web server (otherwise known as assets)
* Incremental execution of fine-grained operations within sbt tasks
* Utilities for managing [Akka](http://akka.io/) from within sbt (sbt-web based plugins use Akka)
* Utilities for managing [WebJars](http://www.webjars.org/) including the ability to extract a WebJar's contents on to disk
* Standardised reporting of compilation style errors

sbt-web was driven from the desire to factor out client-side web concerns from the [Play Framework](http://www.playframework.com/).
However sbt-web is entirely independent of Play and can be used for any project that uses sbt as its build system.

For an overview of sbt-web: http://www.ustream.tv/recorded/42774873

Available Plugins
-----------------

The following is a list of plugins we know of that are built on sbt-web:

* [sbt-coffeescript](https://github.com/sbt/sbt-coffeescript#sbt-coffeescript)
* [sbt-digest](https://github.com/sbt/sbt-digest#sbt-digest)
* [sbt-gzip](https://github.com/sbt/sbt-gzip#sbt-gzip)
* [sbt-jshint](https://github.com/sbt/sbt-jshint#sbt-jshint)
* [sbt-less](https://github.com/sbt/sbt-less#sbt-less)
* [sbt-rjs](https://github.com/sbt/sbt-rjs#sbt-rjs)

File Directory Layout
---------------------

The following directory layout is declared by sbt-web with an indication of the associated settings:

    + src
    --+ main
    ----+ assets .....(sourceDirectory in Assets)
    ------+ js
    ----+ public .....(resourceDirectory in Assets)
    ------+ css
    ------+ images
    ------+ js
    --+ test
    ----+ assets .....(sourceDirectory in TestAssets)
    ------+ js
    ----+ public .....(resourceDirectory in TestAssets)
    ------+ css
    ------+ images
    ------+ js
 
    + target
    --+ web ............(assets-target)
    ----+ public .......(resourceManaged in Assets)
    ------+ css
    ------+ images
    ------+ js
    ----+ public-test ..(resourceManaged in TestAssets)
    ------+ css
    ------+ images
    ------+ js
    ----+ stage ........(web-staging-directory)


The plugin introduces the notion of `assets` to sbt. Assets are public resources that are intended for client-side
consumption e.g. by a browser. This is also distinct from sbt's existing notion of `resources` as
project resources are generally not made public by a web server.

In sbt, asset source files are considered the source for plugins that process them. When they are processed any resultant
files go into a `public` directory in the classpath.  By configuration, sbt-web apps serve static assets from the `public`
directory on the classpath. For example a CoffeeScript plugin would use files from `unmanagedSources in Assets`
and produce them to `resourceManaged in Assets`.

All assets whether they need processing or are static in nature, will be copied to the resourceManaged destinations.

Assets can be organized however desired within the `assets` directory.

One last thing regarding the public and public-test folders... any WebJar depended on by the project will be automatically
extracted into these folders e.g. target/web/public/lib/jquery/jquery.js. In addition the public-test folder receives
the contents of the public folder as well as test assets. This eases the support of test frameworks given that
all files are locatable from one root.

The "stage" directory is product of processing the asset pipeline and results in files prepared for deployment
to a web server.

Incremental Execution
---------------------

The incremental task API lets tasks run more quickly when they are
called more than once. The idea is to do less work when tasks are
called a second time, by skipping any work that has already been done.
In other words, tasks only perform the "incremental" work that is
necessary since they were last run.

To analyse which work needs to be done, a task's work is broken up
into a number of sub-operations, each of which can be run
independently. Each operation takes input parameters and can read and
write files. The incremental task API keeps a record of which
operations have been run so that that those operations don't need to
be repeated in the future.

Asset Pipeline
--------------
 
There are two categories of sbt-web based plugins:
* those that operate on source files
* those that operate on web assets

Examples of source file plugins can be CoffeeScript, LESS and JSHint. Some of these take a source file and produce a
target web asset e.g. CoffeeScript produces JS files. Plugins in this category are mutually exclusive to each other in
terms of their function i.e. only one CoffeeScript plugin will take CoffeeScript sources and produce target JS files.
In summary, source file plugins produce web assets.

Asset plugins operate on web assets directly. The assets they operate on depend on a "stage" in the asset pipeline.
Examples of web asset plugins including RequireJs optimisation, gzip and md5 hashing.

WebDriver and js-engine
-----------------------

The [WebDriver](https://github.com/typesafehub/webdriver) and
[js-engine](https://github.com/typesafehub/js-engine) projects build on sbt-web and provide a DOM
oriented and DOM-less means of JavaScript execution respectively. sbt-web plugins will use one of the two of these
plugins depending on their DOM requirements.
