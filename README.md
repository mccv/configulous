Configulous
===========

Dynamically evaluates scala files, allowing type safe configuration.

Configulous is an alternative to textual configuration formats such as
YAML, JSON, or .properties files.  Its advantages over these text
formats are

*   Strong typing and compiler checking.  If it doesn't compile and
    doesn't conform to the type you expect, you get an exception
*   The full power of Scala in your config.  You don't have to use
    it.  But you can.

Configulous looks in a directory for a collection of Scala snippets
(not classes or objects), wraps them in an evaluator object,
dynamically compiles them, and then returns you the results.

Example
=======

A not exciting configuration file (Config.scala):

    import com.me.MyApplicationConfig

    val config = new MyApplicationConfig
    config.initialSize = 10
    config.mode = "synchronous"
    ...
    config

And from the sbt console

    sbt console
    scala> import com.twitter.configulous._
    import com.twitter.configulous._
    
    scala> Configulous.config[MyApplicationConfig]      
    res0: MyApplicationConfig = MyApplicationConfig(10, "synchronous")
    
    scala> 

Current Status
==============

No tests.
No real testing.
No running on non-OSX/non-SBT/non-Sun-JDK platforms

The Good News
=============

Shit basically works

