addSbtPlugin("org.scala-js"       % "sbt-scalajs"               % "1.11.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"  % "1.2.0")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"              % "2.4.3")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"             % "0.10.0")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"                % "5.6.0")
addSbtPlugin("com.github.cb372"   % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("ch.epfl.scala"      % "sbt-bloop"                 % "1.4.10")
addSbtPlugin("com.geirsson"       % "sbt-ci-release"            % "1.5.7")
addSbtPlugin("dev.zio"            % "zio-sbt-website"           % "0.1.0+2-08b20824-SNAPSHOT")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.5"

resolvers += Resolver.sonatypeRepo("public")
