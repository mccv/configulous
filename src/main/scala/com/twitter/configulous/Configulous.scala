package com.twitter.configulous

import scala.tools.nsc.{Global, Settings} 
import scala.tools.nsc.reporters.ConsoleReporter
import scala.runtime._
import java.io.{File, FileWriter}
import java.net.{URL, URLClassLoader}
import scala.io.Source


/**
 * Trait implemented by the config rewriter.
 * config() is effectively eval()
 *
 * Config looks at all files in a specified config dir,
 * wraps them in a ConfigLoader and writes new .scala files
 * to a specified output dir.
 *
 * It then compiles all the generated classes and loads them.
 * After compilation and loading, it creates a new instance of
 * the specified config class, and calls config() on it, returning
 * the result.
 *
 * Because config takes type parameters, it will fail if your config
 * file does not return an object of the expected type.
 */
trait ConfigLoader {
  def config(): Any
}

/**
 * Dynamically evaluates scala files that build config objects
 */
object Configulous {

  /**
   * configure with defaults.
   * The config file name is defined in the system property config.fileName
   * The config directory is config
   * The generated config directory is gen-config
   */
  def config[T](): T = {
    val configFile = System.getProperty("config.fileName", "Config")
    config[T]("config", "gen-config", configFile)
  }

  /**
   * configure with explicit paths
   */
  def config[T](configDirName: String, targetDirName: String, confFileName: String): T = {
    val configDir = new File(configDirName)
    val targetDir = new File(targetDirName)
    if (targetDir.exists) {
      targetDir.delete
    }
    targetDir.mkdir
    // wraps definitions in a ConfigLoader object, allowing us to eval
    rewriteClasses(configDir, targetDir)
    compile(targetDir)

    // set up the new classloader in targetDir
    val scalaClassLoader = this.getClass.getClassLoader
    val targetDirURL = targetDir.toURL
    val newClassLoader = URLClassLoader.newInstance(Array(targetDir.toURL),  scalaClassLoader)
    val newClasses = loadClasses(targetDir, newClassLoader)
    //println("new classes = %s".format(newClasses))

    // load the generated ConfigLoader and call config()
    val confClassName = loaderName(confFileName + ".scala")
    val confClass = newClasses(confClassName)
    val constructor = confClass.getConstructor()
    val configLoader = constructor.newInstance().asInstanceOf[ConfigLoader]
    configLoader.config.asInstanceOf[T]
  }
  
  def loaderName(fileName: String) = {
    fileName.substring(0, fileName.length - 6) + "Loader"
  }
  /**
   * Wrap all definitions in configDir with a ConfigLoader object,
   * write generated classes to targetDir
   */
  def rewriteClasses(configDir: File, targetDir: File) = {
    val configFiles = configDir.listFiles.filter(_.toString.endsWith(".scala"))
    configFiles.foreach(file => {
      val targetFile = new File(targetDir + File.separator + file.getName)
      if (targetFile.exists) {
        targetFile.delete
      }
      val configLoaderName = loaderName(file.getName)
      targetFile.createNewFile
      val writer = new FileWriter(targetFile)
      
      writer.write("class " + configLoaderName + " extends com.twitter.configulous.ConfigLoader{\n")
      writer.write("  def config = {\n")
      val source = scala.io.Source.fromFile(file)
      source.getLines.foreach(line => {
        writer.write("    " + line)
      })
      writer.write("  }\n")
      writer.write("}\n")
      writer.close
    })
  }

  def fileNameToClassName(fileName: String) = {
    fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length - 6)
  }

  // large portions here lifted from
  // http://scala-programming-language.1934581.n4.nabble.com/Compiler-API-td1992165.html

  def jarPathOfClass(className: String) = {
    val resource = className.split('.').mkString("/", "/", ".class")
    //println("resource for %s is %s".format(className, resource))
    val path = getClass.getResource(resource).getPath
    val indexOfFile = path.indexOf("file:")
    val indexOfSeparator = path.lastIndexOf('!')
    path.substring(indexOfFile, indexOfSeparator)
  }

  val compilerPath = jarPathOfClass("scala.tools.nsc.Interpreter")
  val libPath = jarPathOfClass("scala.ScalaObject")

  /**
   * compile all the .scala files in configDir, writing .class files to the same dir
   */
  def compile(configDir: File) = {
    val settings = new Settings() 
    val origBootclasspath = settings.bootclasspath.value

    // figure out our app classpath.
    // TODO: there are likely a ton of corner cases waiting here
    val configulousClassLoader = this.getClass.getClassLoader.asInstanceOf[URLClassLoader]
    val configulousClasspath = configulousClassLoader.getURLs.map {
      url =>
        val urlStr = url.toString
        urlStr.substring(5, urlStr.length)
    }.toList
    val bootClassPath = origBootclasspath.split(java.io.File.pathSeparator).toList

    // the classpath for compile is our app path + boot path + make sure we have compiler/lib there
    val pathList = bootClassPath ::: (configulousClasspath ::: List(compilerPath, libPath))
    val pathString = pathList.mkString(java.io.File.pathSeparator)
    settings.bootclasspath.value = pathString
    settings.classpath.value = pathString
    settings.deprecation.value = true // enable detailed deprecation warnings 
    settings.unchecked.value = true // enable detailed unchecked warnings 
    settings.outdir.value = configDir.getName

    // may not want to do this
    val reporter = new ConsoleReporter(settings) 
    
    val compiler = new Global(settings, reporter) 
    
    val configFiles = configDir.listFiles.filter(_.toString.endsWith(".scala"))
    val configFileNames = configFiles.map(_.toString).toList
    //println("compiling " + configFileNames)
    try {
      (new compiler.Run).compile(configFileNames)
    } catch {
      case e => {
        //TODO: handle with grace!
        e.printStackTrace
        throw e
      }
    }
    
    if (reporter.hasErrors || reporter.WARNING.count > 0) { 
      //TODO: handle
    }
  }

  /**
   * Load all the class files in the configDir, return a map of class name -> class object
   */
  def loadClasses(configDir: File, classLoader: ClassLoader) = {
    val classTuples = configDir.listFiles.filter(_.toString.endsWith(".class")).map {
      classFile =>
        val className = fileNameToClassName(classFile.toString)
        (className -> classLoader.loadClass(className))
    }
    Map(classTuples:_*)
  }
}
