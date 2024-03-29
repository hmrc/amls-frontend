import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys.*
import sbt.Keys.*
import sbt.Tests.{Group, SubProcess}
import sbt.*
import com.typesafe.sbt.digest.Import.digest
import com.typesafe.sbt.web.Import.{Assets, pipelineStages}
import uk.gov.hmrc.*
import DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import play.twirl.sbt.Import.TwirlKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "amls-frontend"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;modgiels/.data/..*;view.*;forms.*;config.*;" +
      ".*BuildInfo.;uk.gov.hmrc.BuildInfo;.*Routes;.*RoutesPrefix*;controllers.ExampleController;controllers.testonly.TestOnlyController",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) *)
  .settings(libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always))
  .settings(majorVersion := 4)
  .settings(scoverageSettings)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(scalaVersion := "2.13.12")
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
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings) *)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / parallelExecution := false)
 .settings(
    scalacOptions ++= List(
      "-Yrangepos",
      "-Xlint:-missing-interpolator,_",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-Wconf:cat=unused-imports&src=.*routes.*:s"
    )
  )
  .disablePlugins(JUnitXmlReportPlugin)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map {
    test => new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
}
