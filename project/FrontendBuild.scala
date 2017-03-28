import sbt._

object FrontendBuild extends Build with MicroService {

  import scala.util.Properties.envOrElse

  val appName = "amls-frontend"
  val appVersion = envOrElse("AMLS_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion


  private val playHealthVersion = "2.1.0"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val frontendBootstrapVersion = "7.17.0"
  private val govukTemplateVersion = "5.1.0"
  private val playUiVersion = "7.0.0"
  private val playPartialsVersion = "5.3.0"
  private val playAuthorisedFrontendVersion = "6.3.0"
  private val playConfigVersion = "4.3.0"

  private val httpCachingClientVersion = "6.2.0"
  private val playWhitelistFilterVersion = "2.0.0"

  private val validationVersion = "2.0.1"

  private val playJars = ExclusionRule(organization = "com.typesafe.play")

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "govuk-template" % govukTemplateVersion,

    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthorisedFrontendVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-whitelist-filter" % playWhitelistFilterVersion,

    "io.github.jto" %% "validation-core"      % validationVersion excludeAll playJars,
    "io.github.jto" %% "validation-playjson"  % validationVersion excludeAll playJars,
    "io.github.jto" %% "validation-form"      % validationVersion excludeAll playJars

  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }

  private val scalatestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.8.3"
  private val hmrctestVersion = "2.3.0"
  private val authTestVersion = "3.1.0"

  object Test {
    def apply() = new ScopeDependencies {
      override val scope = "test"
      override lazy val dependencies = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrctestVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalacheck" %% "scalacheck" % "1.12.5" % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope
      )
    }.dependencies
  }

  object It {
    def apply() = new ScopeDependencies {
      override lazy val scope = "it"
      override lazy val dependencies = Seq(
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
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
