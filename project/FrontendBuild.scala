import sbt._
import scala.language.reflectiveCalls

object FrontendBuild extends Build with MicroService {

  import scala.util.Properties.envOrElse

  val appName = "amls-frontend"
  val appVersion = envOrElse("AMLS_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "1.1.0"
  private val govukTemplateVersion = "4.0.0"
  private val playUiVersion = "4.10.0"
  private val httpVerbsVersion = "3.3.0"

  private val frontendBootstrapVersion = "6.4.0"
  private val playPartialsVersion = "4.2.0"
  private val playAuthorisedFrontendVersion = "4.7.0"
  private val playConfigVersion = "2.0.1"
  private val playJsonLoggerVersion = "2.1.1"
  private val httpCachingClientVersion = "5.3.0"
  private val playWhitelistFilterVersion = "1.0.1"

  private val metricsPlayVersion = "0.2.1"
  private val metricsGraphiteVersion = "3.0.2"

  private val validationVersion = "1.1"


  private val playJars = ExclusionRule(organization = "com.typesafe.play")

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "http-verbs" % httpVerbsVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,


    // play-frontend replacement libraries
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthorisedFrontendVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "play-json-logger" % playJsonLoggerVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-whitelist-filter" % playWhitelistFilterVersion,

    "com.kenshoo" %% "metrics-play" % metricsPlayVersion,
    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,

    "io.github.jto" %% "validation-core" % validationVersion excludeAll playJars,
    "io.github.jto" %% "validation-json" % validationVersion excludeAll playJars,
    "io.github.jto" %% "validation-form" % validationVersion excludeAll playJars
  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }


  private val scalatestVersion = "2.2.5"
  private val scalatestPlusPlayVersion = "1.2.0"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.8.3"
  private val hmrctestVersion = "1.6.0"
  private val authTestVersion = "2.4.0"

  object Test {
    def apply() = new ScopeDependencies {
      override val scope = "test"
      override lazy val dependencies = Seq(
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalacheck" %% "scalacheck" % "1.12.5" % scope,
        "org.scalatestplus" %% "play" % scalatestPlusPlayVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrctestVersion % scope
      )
    }.dependencies
  }

  object It {
    def apply() = new ScopeDependencies {
      override lazy val scope = "it"
      override lazy val dependencies = Seq(
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalatestplus" %% "play" % scalatestPlusPlayVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrctestVersion % scope,
        "uk.gov.hmrc" %% "auth-test" % authTestVersion % "test, it"
      )
    }.dependencies
  }

  def apply() = compile ++ Test() ++It()
}


