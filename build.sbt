import sbtcrossproject.CrossPlugin.autoImport.crossProject
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import BuildHelper._

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.dev/zio-interop-scalaz")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net")
      )
    ),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
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
    publish / skip := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )

val zioVersion = "1.0.18"

lazy val interopScalaz7x = crossProject(JSPlatform, JVMPlatform)
  .in(file("interop-scalaz7x"))
  .enablePlugins(BuildInfoPlugin)
  .settings(stdSettings("zio-interop-scalaz7x"))
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .settings(buildInfoSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"    %%% "zio"          % zioVersion,
      "org.scalaz" %%% "scalaz-core"  % "7.3.+"    % Optional,
      "dev.zio"    %%% "zio-test"     % zioVersion % "test",
      "dev.zio"    %%% "zio-test-sbt" % zioVersion % "test"
    )
  )

lazy val interopScalaz7xJVM = interopScalaz7x.jvm

lazy val interopScalaz7xJS = interopScalaz7x.js
  .settings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.3.0" % Test
  )

lazy val docs = project
  .in(file("zio-interop-docs"))
  .settings(
    publish / skip := true,
    moduleName := "zio-interop-docs",
    scalacOptions -= "-Yno-imports",
    scalacOptions -= "-Xfatal-warnings",
    projectName := "ZIO Interop Scalaz",
    mainModuleName := (interopScalaz7xJVM / moduleName).value,
    projectStage := ProjectStage.Development,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(interopScalaz7xJVM),
    docsPublishBranch := "master"
  )
  .enablePlugins(WebsitePlugin)
