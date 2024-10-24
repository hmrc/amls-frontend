import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys.*
import sbt.Keys.*
import sbt.Tests.{Group, SubProcess}
import sbt.*
import com.typesafe.sbt.digest.Import.digest
import com.typesafe.sbt.web.Import.{Assets, pipelineStages}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.DefaultBuildSettings
import play.twirl.sbt.Import.TwirlKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "amls-frontend"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;view.*;config.*;.*BuildInfo.;.*Routes;.*RoutesPrefix*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 5

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) *)
  .settings(libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-java8-compat" % VersionScheme.Always))
  .settings(scoverageSettings)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(routesImport += "models.notifications.ContactType._")
  .settings(routesImport += "utils.Binders._")
  .settings(Global / lintUnusedKeysOnLoad := false)
  .settings(
    libraryDependencies ++= appDependencies,
    dependencyOverrides ++= Seq("ch.qos.logback" % "logback-classic" % "1.3.0"),
    retrieveManaged := true,
    PlayKeys.playDefaultPort := 9222,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator,
    Assets / pipelineStages := Seq(digest),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "controllers.routes._"
    )
  )
 .settings(
    scalacOptions ++= List(
      "-Yrangepos",
      "-Xlint:-missing-interpolator,_",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-Wconf:cat=unused-imports&src=.*routes.*:s",
      "-Wconf:msg=legacy-binding:s",
      "-nowarn"
    )
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(commands ++= SbtCommands.commands)


lazy val it = project
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    DefaultBuildSettings.itSettings(true),
    Test / Keys.fork := false,
    addTestReportOption(Test, "int-test-reports"),
    Test / parallelExecution := false
  )
