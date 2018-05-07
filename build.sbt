lazy val akkaHttpVersion = "10.0.11"
lazy val akkaVersion    = "2.5.11"

lazy val jsonParsingLibrary =
  "com.typesafe.play" %% "play-json" % "2.6.9"

lazy val utils =
  Seq(
    "net.codingwell" %% "scala-guice" % "4.1.0",
    "io.igl" %% "jwt" % "1.2.0",
    "de.svenkubiak" % "jBCrypt" % "0.4.1",
    "org.byrde" % "commons_2.11" % "1.0.95")

lazy val orm =
  Seq(
    "com.typesafe.slick" % "slick-hikaricp_2.11" % "3.2.0",
    "com.typesafe.slick" % "slick_2.11" % "3.2.0")

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
      name := "simple-reviews",
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
          "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.18",
          "de.heikoseeberger"  %% "akka-http-play-json"    % "1.17.0",
          "ch.qos.logback"     % "logback-classic"         % "1.2.3",

          "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
          "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
          "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
          "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test) ++ utils ++ orm ++ postgresql :+ jsonParsingLibrary).enablePlugins(JavaAppPackaging)

Revolver.settings
