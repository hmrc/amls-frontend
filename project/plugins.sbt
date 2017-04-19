resolvers += Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
resolvers += Resolver.bintrayRepo("hmrc", "releases")

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

val hmrcRepoHost = java.lang.System.getProperty("hmrc.repo.host", "https://nexus-preview.tax.service.gov.uk")
resolvers ++= Seq("hmrc-snapshots" at hmrcRepoHost + "/content/repositories/hmrc-snapshots",
 "hmrc-releases" at hmrcRepoHost + "/content/repositories/hmrc-releases",
 "typesafe-releases" at hmrcRepoHost + "/content/repositories/typesafe-releases",
 "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/")

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.4.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.0.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "0.9.0")

addSbtPlugin("uk.gov.hmrc" % "hmrc-resolvers" % "0.4.0")

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.5.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.11")

