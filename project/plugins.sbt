addSbtPlugin("org.scala-js"       % "sbt-scalajs"               % "1.5.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"  % "1.0.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"              % "2.4.2")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"             % "0.10.0")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"                % "5.6.0")
addSbtPlugin("com.github.cb372"   % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("ch.epfl.scala"      % "sbt-bloop"                 % "1.4.6")
addSbtPlugin("com.geirsson"       % "sbt-ci-release"            % "1.5.7")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.3"
