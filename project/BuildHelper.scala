import sbt._
import Keys._

import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.CrossType
import sbtbuildinfo._
import BuildInfoKeys._

object BuildHelper {
  private val SilencerVersion = "1.7.1"

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
    crossScalaVersions := Seq("2.13.3", "2.12.12", "2.11.12"),
    scalaVersion in ThisBuild := crossScalaVersions.value.head,
    scalacOptions := stdOptions ++ extraOptions(scalaVersion.value),
    libraryDependencies ++= compileOnlyDeps ++ Seq(
      "com.github.ghik" % "silencer-lib" % SilencerVersion % Provided,
      compilerPlugin("com.github.ghik" % "silencer-plugin" % SilencerVersion),
      compilerPlugin("org.typelevel"   %% "kind-projector"  % "0.10.3")
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
