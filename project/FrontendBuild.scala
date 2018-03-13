import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "amls-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion


  private val frontendBootstrapVersion = "8.19.0"
  private val playPartialsVersion = "6.1.0"
  private val httpCachingClientVersion = "7.1.0"
  private val playWhitelistFilterVersion = "2.0.0"
  private val validationVersion = "2.0.1"
  private val flexmarkVersion = "0.19.1"
  private val okHttpVersion = "3.9.1"

  private val playJars = ExclusionRule(organization = "com.typesafe.play")

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-whitelist-filter" % playWhitelistFilterVersion,

    "io.github.jto" %% "validation-core"      % validationVersion excludeAll playJars,
    "io.github.jto" %% "validation-playjson"  % validationVersion excludeAll playJars,
    "io.github.jto" %% "validation-form"      % validationVersion excludeAll playJars,

    "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion,
    "com.beachape" %% "enumeratum-play" % "1.5.10",
    "com.squareup.okhttp3" % "mockwebserver" % okHttpVersion
  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }

  private val scalatestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.9.2"
  private val hmrctestVersion = "2.4.0"

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

  def apply() = compile ++ Test()
}
