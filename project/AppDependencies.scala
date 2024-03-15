import sbt._

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playPartialsVersion = "8.3.0-play-28"
  private val httpCachingClientVersion = "11.2.0"
  private val flexmarkVersion = "0.19.1"
  private val okHttpVersion = "4.12.0"
  private val jsonEncryptionVersion = "5.3.0-play-28"
  private val hmrcMongoVersion = "0.71.0"
  private val domain = "8.1.0-play-28"
  private val bootstrapV = "7.23.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% "domain"                        % domain,
    "uk.gov.hmrc"       %% "play-partials"                 % playPartialsVersion,
    "uk.gov.hmrc"       %% "http-caching-client-play-28"   % httpCachingClientVersion,
    "uk.gov.hmrc"       %% "json-encryption"               % jsonEncryptionVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootstrapV,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-28"    % "8.4.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",

    "com.vladsch.flexmark"   % "flexmark-all"     % flexmarkVersion,
    "com.beachape"          %% "enumeratum-play"  % "1.6.3",
    "com.squareup.okhttp3"   % "mockwebserver"    % okHttpVersion,
    "com.typesafe.play"     %% "play-json"        % "2.8.1",
    "com.typesafe.play"     %% "play-json-joda"   % "2.8.1",
    "org.typelevel"         %% "cats-core"        % "2.10.0",
    "commons-codec"          % "commons-codec"    % "1.15",

    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.13" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.13" % Provided cross CrossVersion.full
  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }

  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.17.2"

  object Test {
    def apply() = new ScopeDependencies {
      override val scope = "test"
      override lazy val dependencies = Seq(
        "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapV          % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"      % "5.0.0"             % scope,
        "org.scalacheck"         %% "scalacheck"              % "1.14.3"            % scope,
        "org.pegdown"             % "pegdown"                 % pegdownVersion      % scope,
        "org.jsoup"               % "jsoup"                   % jsoupVersion        % scope,
        "com.typesafe.play"      %% "play-test"               % PlayVersion.current % scope,
        "org.mockito"             % "mockito-all"             % "1.10.19"           % scope,
      )
    }.dependencies
  }

  def apply() = compile ++ Test()
}
