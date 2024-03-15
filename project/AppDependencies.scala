import sbt._

private object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playV = "play-28"
  private val flexmarkVersion = "0.64.8"
  private val bootstrapV = "7.23.0"

  val compile = Seq(
    ws,
    // GOV UK
    "uk.gov.hmrc"           %% "domain"                        % s"8.1.0-$playV",
    "uk.gov.hmrc"           %% "play-partials"                 % s"8.3.0-$playV",
    "uk.gov.hmrc"           %% s"http-caching-client-$playV"   % "11.2.0",
    "uk.gov.hmrc"           %% "json-encryption"               % s"5.3.0-$playV",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playV"            % "0.71.0",
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playV"    % bootstrapV,
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playV"    % "8.4.0",
    "uk.gov.hmrc"           %% "play-conditional-form-mapping" % s"1.13.0-$playV",
    // OTHER
    "com.vladsch.flexmark"   % "flexmark-all"                  % flexmarkVersion,
    "com.beachape"          %% "enumeratum-play"               % "1.6.3",
    "com.squareup.okhttp3"   % "mockwebserver"                 % "4.12.0",
    "org.typelevel"         %% "cats-core"                     % "2.10.0",
    "commons-codec"          % "commons-codec"                 % "1.15",
    // PLAY
    "com.typesafe.play"     %% "play-json"                     % "2.8.1",
    "com.typesafe.play"     %% "play-json-joda"                % "2.8.1",

    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.13" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.13" % Provided cross CrossVersion.full
  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }

  object Test {
    def apply() = new ScopeDependencies {
      override val scope = "test"
      override lazy val dependencies = Seq(
        "uk.gov.hmrc"            %% s"bootstrap-test-$playV"  % bootstrapV          % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1"             % scope,
        "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.11.0"          % scope,
        "org.scalacheck"         %% "scalacheck"              % "1.17.0"            % scope,
        "org.jsoup"               % "jsoup"                   % "1.17.2"            % scope,
        "com.typesafe.play"      %% "play-test"               % PlayVersion.current % scope,
        "org.mockito"             % "mockito-all"             % "1.10.19"           % scope,
        "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.8"            % scope
      )
    }.dependencies
  }

  def apply() = compile ++ Test()
}
