sbt-web
=======

This project provides the building blocks for web oriented sbt plugins by bringing together the following concerns:

* file directory layout conventions for resources intended to be served by a web server (otherwise known as assets);
* incremental execution of fine-grained operations within sbt tasks;
* utilities for managing [Akka](http://akka.io/) from within sbt (sbt-web based plugins use Akka);
* utilities for managing [WebJars](http://www.webjars.org/) including the ability to flatten a WebJar's contents on to disk; and
* standardised reporting of compilation style errors.

sbt-web was driven from the desire to factor out web concerns from the [Play framework](http://www.playframework.com/).
However sbt-web is entirely independent of Play and can be used for any project that uses sbt as its build system.

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
    --+ public .......(resourceManaged in Assets)
    ----+ css
    ----+ images
    ----+ js
    --+ public-test ..(resourceManaged in TestAssets)
    ----+ css
    ----+ images
    ----+ js

The plugin introduces the notion of "assets" to sbt. Assets are public resources that are intended for client-side
consumption e.g. by a browser. This is also distinct from sbt's existing notion of "resources" as
project resources are generally not made public by a web server. The name "assets" heralds from Rails.

"public" denotes a type of asset that does not require processing i.e. these resources are static in nature.

In sbt, asset source files are considered the source for plugins that process them. When they are processed any resultant
files become public. For example a coffeescript plugin would use files from "unmanagedSources in Assets" and produce them to
"resourceManaged in Assets".

All assets be them subject to processing or static in nature, will be copied to the resourceManaged destinations.

How files are organised within "assets" or "public" is subject to the taste of the developer, their team and
conventions at large.

Incremental Execution
---------------------

The incremental task API lets tasks run more quickly when they are
called more than once. The idea is to do less work when tasks are
called a second time, by skipping any work that has already been done.
In other words, tasks only perform the “incremental” work that is
necessary since they were last run.

To analyse which work needs to be done, a task’s work is broken up
into a number of sub-operations, each of which can be run
independently. Each operation takes input parameters and can read and
write files. The incremental task API keeps a record of which
operations have been run so that that those operations don’t need to
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

Asset plugins operate on web assets directly. The assets they operate on depend on a “stage” in the asset pipeline.
Examples of web asset plugins including RequireJs optimisation, gzip and md5 hashing.

WebDriver and js-engine
-----------------------

The [WebDriver](https://github.com/typesafehub/webdriver#webdriver) and
[js-engine](https://github.com/typesafehub/js-engine#javascript-engine) projects build on sbt-web and provide a DOM
oriented and DOM-less means of JavaScript execution respectively. sbt-web plugins will use one of the two of these
plugins depending on their DOM requirements.