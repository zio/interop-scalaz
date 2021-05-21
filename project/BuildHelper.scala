import sbt._
import Keys._

import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.CrossType
import sbtbuildinfo._
import BuildInfoKeys._

object BuildHelper {
  private val versions: Map[String, String] = {
    import org.snakeyaml.engine.v2.api.{ Load, LoadSettings }

    import java.util.{ List => JList, Map => JMap }
    import scala.jdk.CollectionConverters._

    val doc  = new Load(LoadSettings.builder().build())
      .loadFromReader(scala.io.Source.fromFile(".github/workflows/ci.yml").bufferedReader())
    val yaml = doc.asInstanceOf[JMap[String, JMap[String, JMap[String, JMap[String, JMap[String, JList[String]]]]]]]
    val list = yaml.get("jobs").get("test").get("strategy").get("matrix").get("scala").asScala
    list.map(v => (v.split('.').take(2).mkString("."), v)).toMap
  }

  val Scala211: String = versions("2.11")
  val Scala212: String = versions("2.12")
  val Scala213: String = versions("2.13")

  private val stdOptions = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  )

  private val std2xOptions = Seq(
    "-Xfatal-warnings",
    "-language:higherKinds",
    "-language:existentials",
    "-explaintypes",
    "-Yrangepos",
    "-Xsource:2.13",
    "-Xlint:_,-type-parameter-shadow",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )

  private val optimizerOptions =
    Seq("-opt:l:inline", "-opt-inline-from:zio.internal.**")

  val buildInfoSettings = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, isSnapshot),
    buildInfoPackage := "zio",
    buildInfoObject := "BuildInfo"
  )

  def extraOptions(scalaVersion: String) =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        std2xOptions ++ optimizerOptions
      case Some((2, 12)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused:_,imports",
          "-Ywarn-unused:imports",
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit"
        ) ++ std2xOptions ++ optimizerOptions
      case Some((2, 11)) =>
        Seq(
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xexperimental",
          "-Ywarn-unused-import"
        ) ++ std2xOptions
      case _             => Seq.empty
    }

  def stdSettings(prjName: String) = Seq(
    name := s"$prjName",
    scalacOptions := stdOptions,
    crossScalaVersions := Seq(Scala213, Scala212, Scala211),
    scalaVersion in ThisBuild := crossScalaVersions.value.head,
    scalacOptions := stdOptions ++ extraOptions(scalaVersion.value),
    libraryDependencies ++= Seq(
      compilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
    ),
    parallelExecution in Test := true,
    incOptions ~= (_.withLogRecompileOnMacro(false)),
    autoAPIMappings := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library"),
    Compile / unmanagedSourceDirectories ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, x)) if x <= 11 =>
          CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + "-2.11")) ++
            CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + "-2.11"))
        case Some((2, x)) if x >= 12 =>
          CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + "-2.12+")) ++
            CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + "-2.12+"))
        case _                       => Nil
      }
    },
    Test / unmanagedSourceDirectories ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, x)) if x <= 11 =>
          Seq(file(sourceDirectory.value.getPath + "/test/scala-2.11"))
        case Some((2, x)) if x >= 12 =>
          Seq(
            file(sourceDirectory.value.getPath + "/test/scala-2.12"),
            file(sourceDirectory.value.getPath + "/test/scala-2.12+")
          )
        case _                       => Nil
      }
    }
  )
}
