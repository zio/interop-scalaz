import sbtcrossproject.CrossPlugin.autoImport.crossProject
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import ScalazBuild._

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.dev")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net")
      )
    ),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    releaseEarlyWith := SonatypePublisher,
    scmInfo := Some(
      ScmInfo(url("https://github.com/zio/interop-scalaz/"), "scm:git:git@github.com:zio/interop-scalaz.git")
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("testJVM", ";interopScalaz7xJVM/test")
addCommandAlias("testJS", ";interopScalaz7xJS/test")

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .aggregate(interopScalaz7xJVM, interopScalaz7xJS)
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )

lazy val interopScalaz7x = crossProject(JSPlatform, JVMPlatform)
  .in(file("interop-scalaz7x"))
  .enablePlugins(BuildInfoPlugin)
  .settings(stdSettings("zio-interop-scalaz7x"))
  .settings(buildInfoSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"    %%% "zio"                       % "1.0.0-RC11-1",
      "org.scalaz" %%% "scalaz-core"               % "7.2.+" % Optional,
      "dev.zio"    %%% "core-tests"                % "1.0.0-RC11-1" % Test classifier "tests",
      "org.specs2" %%% "specs2-core"               % "4.7.0" % Test,
      "org.specs2" %%% "specs2-scalacheck"         % "4.7.0" % Test,
      "org.specs2" %%% "specs2-matcher-extra"      % "4.7.0" % Test,
      "org.scalaz" %%% "scalaz-scalacheck-binding" % "7.2.+" % Test
    )
  )

lazy val interopScalaz7xJVM = interopScalaz7x.jvm

lazy val interopScalaz7xJS = interopScalaz7x.js
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.5" % Test
  )
