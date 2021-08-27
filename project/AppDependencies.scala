import sbt._

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playPartialsVersion = "8.2.0-play-28"
  private val httpCachingClientVersion = "9.5.0-play-28"
  private val validationVersion = "2.1.0"
  private val flexmarkVersion = "0.19.1"
  private val okHttpVersion = "3.9.1"
  private val jsonEncryptionVersion = "4.10.0-play-28"
  private val playReactivemongoVersion = "8.0.0-play-28"
  private val domain = "6.2.0-play-28"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "domain" % domain,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "json-encryption" % jsonEncryptionVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % playReactivemongoVersion,
    "uk.gov.hmrc" %% "play-ui" % "9.6.0-play-28",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.12.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.69.0-play-28",

    "io.github.jto" %% "validation-core"      % validationVersion,
    "io.github.jto" %% "validation-playjson"  % validationVersion,
    "io.github.jto" %% "validation-form"      % validationVersion,

    "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion,
    "com.beachape" %% "enumeratum-play" % "1.5.15",
    "com.squareup.okhttp3" % "mockwebserver" % okHttpVersion,
    "com.typesafe.play" %% "play-json" % "2.9.2",
    "com.typesafe.play" %% "play-json-joda" % "2.9.2",
    
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.5" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.5" % Provided cross CrossVersion.full
  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }

  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.13.1"

  object Test {
    def apply() = new ScopeDependencies {
      override val scope = "test"
      override lazy val dependencies = Seq(
        "org.scalacheck" %% "scalacheck" % "1.14.3" % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % scope
      )
    }.dependencies
  }

  def apply() = compile ++ Test()
}
