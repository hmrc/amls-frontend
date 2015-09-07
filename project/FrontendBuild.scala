import sbt._

object FrontendBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "amls-frontend"
  val appVersion = envOrElse("AMLS_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "0.7.0"    

  private val govukTemplateVersion = "2.6.0"
  private val playUiVersion = "1.8.0"
  
  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,

    // play-frontend replacement libraries
    "uk.gov.hmrc" %% "frontend-bootstrap" % "1.0.0",
    "uk.gov.hmrc" %% "play-partials" % "1.6.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "1.5.0",
    "uk.gov.hmrc" %% "play-config" % "1.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "1.0.0",

    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.1"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "0.4.0" % scope,
        "org.scalatest" %% "scalatest" % "2.2.5" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.jsoup" % "jsoup" % "1.7.2" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}


