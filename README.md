Configulous
===========

Dynamically evaluates scala files, allowing type safe configuration.

Example
=======

   sbt console
   scala> import com.twitter.configulous._
   import com.twitter.configulous._

   scala> Configulous.config[String]      
   res0: String = classpath: /Users/mmcbride/bin/sbt-launcher.jar

   scala> 

Current Status
==============

No tests.
No real testing.
No running on non-OSX/non-SBT/non-Sun-JDK platforms

The Good News
=============

Shit basically works

