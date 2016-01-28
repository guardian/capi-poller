name := """capi-poller"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, JavaAppPackaging)

mappings in Universal ++= (baseDirectory.value / "resources" *** ).get pair relativeTo(baseDirectory.value)

riffRaffPackageType := (packageZipTarball in config("universal")).value

addCommandAlias("dist", ";riffRaffArtifact")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-agent" % "2.3.4",
  "com.gu" %% "content-api-client" % "7.10",
  "com.gu" %% "configuration" % "4.0",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  ws,
  specs2 % Test
)
routesGenerator := InjectedRoutesGenerator