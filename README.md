sbt-web
=======

[![Build Status](https://github.com/sbt/sbt-web/actions/workflows/build-test.yml/badge.svg)](https://github.com/sbt/sbt-web/actions/workflows/build-test.yml)

This project provides the building blocks for web oriented sbt plugins by bringing together the following concerns:

* File directory layout conventions for resources intended to be served by a web server (otherwise known as assets)
* Incremental execution of fine-grained operations within sbt tasks
* Utilities for managing [WebJars](http://www.webjars.org/) including the ability to extract a WebJar's contents on to disk
* Standardised reporting of compilation style errors

sbt-web was driven from the desire to factor out client-side web concerns from the [Play Framework](http://www.playframework.com/).
However, sbt-web is entirely independent of Play and can be used for any project that uses sbt as its build system.

Available Plugins
-----------------

The following is a list of plugins we know of that are built on sbt-web:

* [sbt-autoprefixer](https://github.com/matthewrennie/sbt-autoprefixer)
* [sbt-closure](https://github.com/ground5hark/sbt-closure#sbt-closure)
* [sbt-coffeescript](https://github.com/sbt/sbt-coffeescript#sbt-coffeescript)
* [sbt-coffeescript-reactjs](https://github.com/ShaggyYeti/sbt-coffeescript-reactjs)
* [sbt-concat](https://github.com/sbt/sbt-concat#sbt-concat)
* [sbt-css-compress](https://github.com/ground5hark/sbt-css-compress#sbt-css-compress)
* [sbt-digest](https://github.com/sbt/sbt-digest#sbt-digest)
* [sbt-dustjs-linkedin](https://github.com/jmparsons/sbt-dustjs-linkedin)
* [sbt-emberjs](https://github.com/dwickern/sbt-emberjs)
* [sbt-filter](https://github.com/rgcottrell/sbt-filter)
* [sbt-gzip](https://github.com/sbt/sbt-gzip#sbt-gzip)
* [sbt-handlebars](https://github.com/Amadeus82/sbt-handlebars)
* [sbt-hbs](https://github.com/bicouy0/sbt-hbs)
* [sbt-html-js-wrap](https://github.com/kolloch/sbt-html-js-wrap)
* [sbt-html-minifier](https://github.com/rgcottrell/sbt-html-minifier)
* [sbt-imagemin](https://github.com/rgcottrell/sbt-imagemin)
* [sbt-jasmine](https://github.com/joost-de-vries/sbt-jasmine)
* [sbt-jshint](https://github.com/sbt/sbt-jshint#sbt-jshint)
* [sbt-less](https://github.com/sbt/sbt-less#sbt-less)
* [sbt-mocha](https://github.com/sbt/sbt-mocha)
* [sbt-purescript](https://github.com/eamelink/sbt-purescript)
* [sbt-reactjs](https://github.com/ddispaltro/sbt-reactjs)
* [sbt-rjs](https://github.com/sbt/sbt-rjs#sbt-rjs)
* [sbt-sass](https://github.com/madoushi/sbt-sass)
* [sbt-sassify](https://github.com/irundaia/sbt-sassify)
* [sbt-puresass](https://chiselapp.com/user/twenstar/repository/sbt-puresass)
* [sbt-simple-url-update](https://github.com/neomaclin/sbt-simple-url-update#sbt-simple-url-update)
* [sbt-stylus](https://github.com/sbt/sbt-stylus)
* [sbt-terser](https://github.com/andriimartynov/sbt-terser)
* [sbt-traceur](https://github.com/LuigiPeace/sbt-traceur)
* [sbt-tslint](https://github.com/joost-de-vries/sbt-tslint)
* [sbt-typescript](https://github.com/ArpNetworking/sbt-typescript)
* [sbt-typescript](https://github.com/joost-de-vries/sbt-typescript) (archived)
* [sbt-uglify](https://github.com/sbt/sbt-uglify)
* [sbt-vuefy](https://github.com/GIVESocialMovement/sbt-vuefy)
* [sbt-web-brotli](https://github.com/dwickern/sbt-web-brotli)
* [sbt-web-scalajs](https://github.com/vmunier/sbt-web-scalajs)

Ideas for Plugins
-----------------

* [jasmine](http://jasmine.github.io/)
* [handlebars](http://handlebarsjs.com/)

Plugins in Development
----------------------
* C'mon community, get involved! Watch the talk on the anatomy of an [sbt-web plugin](https://www.youtube.com/watch?v=lIznJSBW-GU)

File Directory Layout
---------------------

The following directory layout is declared by sbt-web with an indication of the associated settings:

    + src
    --+ main
    ----+ assets .....(Assets / sourceDirectory)
    ------+ js
    ----+ public .....(Assets / resourceDirectory)
    ------+ css
    ------+ images
    ------+ js
    --+ test
    ----+ assets .....(TestAssets / sourceDirectory)
    ------+ js
    ----+ public .....(TestAssets / resourceDirectory)
    ------+ css
    ------+ images
    ------+ js
 
    + target
    --+ web ............(webTarget)
    ----+ public
    ------+ main .......(Assets / resourceManaged)
    --------+ css
    --------+ images
    --------+ js
    ------+ test .......(TestAssets / resourceManaged)
    --------+ css
    --------+ images
    --------+ js
    ----+ stage ........(stagingDirectory)


The plugin introduces the notion of `assets` to sbt. Assets are public resources that are intended for client-side
consumption e.g. by a browser. This is also distinct from sbt's existing notion of `resources` as
project resources are generally not made public by a web server.

In sbt, asset source files are considered the source for plugins that process them. When they are processed any resultant
files go into a `public` directory in the classpath.  By configuration, sbt-web apps serve static assets from the `public`
directory on the classpath. For example a CoffeeScript plugin would use files from `Assets / sourceDirectory`
and produce them to `Assets / resourceManaged`.

All assets whether they need processing or are static in nature, will be copied to the resourceManaged destinations.

Assets can be organized however desired within the `assets` directory.

One last thing regarding the public and public-test folders... any WebJar depended on by the project will be automatically
extracted into these folders e.g. target/web/public/main/lib/jquery/jquery.js. In addition, the public-test folder receives
the contents of the public folder as well as test assets. This eases the support of test frameworks given that
all files are locatable from one root.

The "stage" directory is product of processing the asset pipeline and results in files prepared for deployment
to a web server.

Configurations
--------------

sbt holds the notion of configurations which are similar to Maven's phases. Configurations aggregate settings and tasks. Familiar configurations will be `Compile` and `Test`. sbt-web introduces two new configurations named `Assets` and `TestAssets` correlating roughly with `Compile` and `Test`, but for web assets.

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
operations have been run so that those operations don't need to
be repeated in the future.

Asset Pipeline
--------------
 
There are two categories of sbt-web based tasks:
* those that operate on source files
* those that operate on web assets

Examples of source file tasks as plugins are CoffeeScript, LESS and JSHint. Some of these take a source file and produce a
target web asset e.g. CoffeeScript produces JS files. Plugins in this category are mutually exclusive to each other in
terms of their function i.e. only one CoffeeScript plugin will take CoffeeScript sources and produce target JS files.
In summary, source file plugins produce web assets.

Asset tasks operate on web assets directly. The assets they operate on depend on a "stage" in the asset pipeline.
Examples of web asset tasks as plugins include RequireJs optimisation, gzip and md5 hashing.

Source file tasks can be considered to provide files for the first stage of asset pipeline processing and they will be
executed often e.g. for each compilation of your project's source files. Asset pipeline tasks are generally executed at the time
that you wish to prepare a distribution for deployment into, say, production.

Exporting Assets
----------------

Assets are automatically available across subproject dependencies using sbt's
classpath setup. The assets are exported in the webjar format and are imported
in the same way as other webjar dependencies.

So given a project dependency like this, where project A depends on project B:

```scala
lazy val a = (project in file("a"))
  .enablePlugins(SbtWeb)
  .dependsOn(b)

lazy val b = (project in file("b"))
  .enablePlugins(SbtWeb)
```

Assets from project B are available to project A under `lib/b/`.

The module name for imported assets is the same as the project module name (the
normalized name of the project). This can be changed with the
`Assets / moduleName` setting.

Test assets are also exported if a test dependency is specified. For example:

```scala
lazy val a = (project in file("a"))
  .enablePlugins(SbtWeb)
  .dependsOn(b % "compile;test->test")
```

### Packaging and Publishing

*The packaging and publishing behavior is documented for sbt-web version `1.1.0` and above.*

Assets are automatically packaged and published along with project classes
at the following path inside the jar: `META-INF/resources/webjars/module/version/`.
This means that assets can be shared as external library dependencies. Simply publish
the project and use as a library dependency. The assets will be extracted and
available under `lib/module/` in the same way as other webjar dependencies or
internal dependencies.

To package all assets for production there is a `Assets / packageBin` task,
`web-assets:package` in the sbt shell. This packages the result of the asset
pipeline. An optional path prefix can be specified with the `packagePrefix in
Assets` setting. For example, to have assets packaged under a `public`
directory, as used for Play Framework distributions:

```scala
Assets / WebKeys.packagePrefix := "public/"
```

To automatically add the production-ready assets to classpath, the following might be useful:

```scala
(Runtime / managedClasspath) += (Assets / packageBin).value
```

Writing a Source File task
--------------------------

The following represents the minimum amount of code required in a `build.sbt` to create a task that operates on source files i.e.
those files that are available for processing from `src/main/assets`. Source file tasks are
resource generators in sbt terms.

```scala
val mySourceFileTask = taskKey[Seq[File]]("Some source file task")

mySourceFileTask := Nil

Assets / sourceGenerators += mySourceFileTask.taskValue
```

The addition of the `mySourceFileTask` to `Assets / sourceGenerators` declares the task as a resource generator and,
as such, will be executed in parallel with other resource generators during the `WebKeys.assets` task execution.
Using sbt's `show` command will yield the directory where all source file assets have been written to e.g.:

```scala
show web-assets:assets
```

Source file tasks take input, typically from `Assets / sourceDirectory` (and/or `TestAssets`) and produce a sequence
of files that have been generated from that input.

The following code illustrates a more complete example where input files matching *.coffee are taken and copied to an 
output folder:

```scala
mySourceFileTask := {
  // translate .coffee files into .js files
  val sourceDir = (Assets / sourceDirectory).value
  val targetDir = webTarget.value / "cs-plugin"
  val sources = sourceDir ** "*.coffee"
  val mappings = sources pair relativeTo(sourceDir)
  val renamed = mappings map { case (file, path) => file -> path.replaceAll("coffee", "js") }
  val copies = renamed map { case (file, path) => file -> (Assets / resourceManaged).value / path }
  IO.copy(copies)
  copies map (_._2)
}
```

Using the `WebKeys.assets` task will perform source file tasks in parallel. If you find yourself using a
source file task across many projects then consider wrapping it with an sbt plugin. Example source file plugins 
are `sbt-jshint`, `sbt-coffeescript` and `sbt-stylus`.

As a final note, if you plugin depends on node modules e.g. those that are extracted from WebJars or from NPM,
then you will need to have your task depend on the node module extraction task. The following illustrates how
given the `Assets` scope:

```scala
mySourceFileTask := Def.task {
  Nil
}.dependsOn(Assets / WebKeys.nodeModules).value
```

If you're wrapping the task within a plugin then you will need the Plugin's scope as opposed to the `Assets`
scope i.e.:

```scala
mySourceFileTask := Def.task {
  Nil
}.dependsOn(Plugin / WebKeys.nodeModules).value
```

Writing an Asset Pipeline task
------------------------------

The following represents the minimum amount of code required to create a pipeline stage in a `build.sbt` file:

```scala
import com.typesafe.sbt.web.pipeline.Pipeline

val myPipelineTask = taskKey[Pipeline.Stage]("Some pipeline task")

myPipelineTask := identity

pipelineStages := Seq(myPipelineTask)
```

`myPipelineTask` is a function that receives a `Seq[PathMapping]` and produces a `Seq[PathMapping]`. `PathMapping` is a tuple
of `(File, String)` where the first member provides the full path to a file, and the second member declares the portion of that
path which is to be considered relative. For example `(file("/a/b/c"), "b/c"). `PathMapping` types are commonly used in sbt and
are useful in terms of providing access to a file and preserving information about its relative path; the latter being typically
useful for copying files to a target folder where the relative portion of the path must be retained.

In the above example an identity function is used i.e. what is passed in is simply returned. The task is included within a 
sequence and assigned to `pipelineStages` i.e. the sequence represents the asset pipeline. Each stage in the asset pipeline is 
executed after any previous stage has completed. A stage therefore receives the product of files any previous stage as input. 
A stage's output then becomes the input to any subsequent stage. The first stage will always receive the output of having 
executed source file tasks as its input.

If you have some need for the assets produced by the `pipelineStages` in your development environment (during `play run`), then you can scope the `pipelineStages` to the `Assets` config.

```scala
Assets / pipelineStages := Seq(myPipelineTask)
```

To perform the asset pipeline tasks use the `WebKeys.stage` task. If you use sbt's `show` command from the console then you will see the directory that the pipeline has been written to e.g.:

```scala
show web-stage
```

Returning what is passed in is not particularly useful. Stages tend to add and remove files from the input as expressed in
the output returned. The following expanded task simulates minifying some js files and consequently adds files to the pipeline:

```scala
myPipelineTask := { mappings: Seq[PathMapping] =>
  // pretend to combine all .js files into one .min.js file
  val targetDir = webTarget.value / "myPipelineTask" / "target"
  val (js, other) = mappings partition (_._2.endsWith(".js"))
  val minFile = targetDir / "js" / "all.min.js"
  IO.touch(minFile)
  val minMappings = Seq(minFile) pair relativeTo(targetDir)
  minMappings ++ other
}
```

If you find yourself commonly using a pipeline stage task across projects then you should consider wrapping it with an sbt
plugin. Examples of such plugins are `sbt-digest`, `sbt-gzip`, `sbt-rjs` and `sbt-uglify`. The first two illustrate stages
implemented using JVM based libraries while the latter two illustrate invoking JavaScript via js-engine.

WebDriver and js-engine
-----------------------

The [WebDriver](https://github.com/typesafehub/webdriver) and
[js-engine](https://github.com/typesafehub/js-engine) projects build on sbt-web and provide a DOM
oriented and DOM-less means of JavaScript execution respectively. sbt-web plugins will use one of the two of these
plugins depending on their DOM requirements.

# Releasing sbt-web

1. Tag the release: `git tag -s 1.2.3`
1. Push tag: `git push upstream 1.2.3`
1. GitHub action workflow does the rest: https://github.com/sbt/sbt-web/actions/workflows/publish.yml
