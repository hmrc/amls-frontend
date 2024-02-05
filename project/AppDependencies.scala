import sbt.Keys.dependencyOverrides
import sbt.{Def, _}

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playPartialsVersion = "9.1.0"
  private val httpCachingClientVersion = "11.1.0"
  private val flexmarkVersion = "0.64.8"
  private val okHttpVersion = "3.9.1"
  private val jsonEncryptionVersion = "5.1.0-play-28"
  private val hmrcMongoVersion = "1.4.0"
  private val domain = "9.0.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "domain-play-30"                        % domain,
    "uk.gov.hmrc"       %% "play-partials-play-30"                 % playPartialsVersion,
    "uk.gov.hmrc"       %% "http-caching-client-play-30"           % httpCachingClientVersion,
    "uk.gov.hmrc"       %% "json-encryption"                       % jsonEncryptionVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"                    % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"            % "8.0.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"            % "8.4.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30" % "2.0.0",

    "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion,
    "com.beachape" %% "enumeratum-play" % "1.8.0",
    "com.squareup.okhttp3" % "mockwebserver" % okHttpVersion,
    "org.playframework" %% "play-json" % "3.0.2",
    "org.playframework" %% "play-json-joda" % "3.0.2",
    "org.typelevel"     %% "cats-core"      % "2.10.0",
    "commons-codec" % "commons-codec" % "1.15"
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
        "org.playframework" %% "play-test" % "3.0.1" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % scope
      )
    }.dependencies
  }

  def apply() = compile ++ Test()
}
