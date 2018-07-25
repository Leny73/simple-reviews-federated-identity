lazy val akkaHttpVersion = "10.1.0"
lazy val akkaVersion    = "2.5.11"

lazy val jsonParsingLibrary =
  "com.typesafe.play" %% "play-json" % "2.6.9"

lazy val utils =
  Seq(
    "io.igl" %% "jwt" % "1.2.2",
    "de.svenkubiak" % "jBCrypt" % "0.4.1",
    "org.byrde" % "commons_2.11" % "1.0.139")

lazy val guice =
  Seq(
    "net.codingwell" %% "scala-guice" % "4.2.1",
    "com.google.inject" % "guice" % "4.2.0",
    "com.google.inject.extensions" % "guice-assistedinject" % "4.2.0")

lazy val orm =
  Seq(
    "com.typesafe.slick" % "slick-hikaricp_2.11" % "3.2.0",
    "com.typesafe.slick" % "slick_2.11" % "3.2.0")

lazy val redis =
  Seq (
    "org.sedis" %% "sedis" % "1.2.2")

lazy val postgresql =
  Seq (
    "org.postgresql" % "postgresql" % "9.4.1212")

lazy val root =
  (project in file("."))
    .settings(
      inThisBuild(List(
        organization    := "org.simplereviews",
        version         := "1.0",
        scalaVersion    := "2.11.11"
      )),
      name := "simple-reviews-federated-identity",
      scalacOptions ++=
        Seq(
          "-unchecked",
          "-deprecation",
          "-Xlint",
          "-Ywarn-dead-code",
          "-language:_",
          "-target:jvm-1.8",
          "-encoding", "UTF-8"),
      resolvers ++=
        Seq(
          "byrdelibraries" at "https://dl.cloudsmith.io/public/byrde/libraries/maven/",
          Resolver.bintrayRepo("hseeberger", "maven")),
      libraryDependencies ++=
        Seq(
          "com.typesafe.akka"  %% "akka-http"              % akkaHttpVersion,
          "com.typesafe.akka"  %% "akka-http-xml"          % akkaHttpVersion,
          "com.typesafe.akka"  %% "akka-http-caching"      % akkaHttpVersion,
          "com.typesafe.akka"  %% "akka-stream"            % akkaVersion,
          "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.19",
          "de.heikoseeberger"  %% "akka-http-play-json"    % "1.21.0",
          "ch.qos.logback"     % "logback-classic"         % "1.2.3",

          "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
          "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
          "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
          "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test) ++ utils ++ guice ++ orm ++ postgresql :+ jsonParsingLibrary).enablePlugins(JavaAppPackaging)
