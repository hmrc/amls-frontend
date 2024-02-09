import sbt.{Def, _}

private object AppDependencies {

  import play.sbt.PlayImport._

  private final val playV = "play-30"
  private final val playJsonV = "3.0.2"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"          %% s"domain-$playV"                        % "9.0.0",
    "uk.gov.hmrc"          %% s"play-partials-$playV"                 % "9.1.0",
    "uk.gov.hmrc"          %% s"http-caching-client-$playV"           % "11.1.0",
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playV"                    % "1.4.0",
    "uk.gov.hmrc"          %% s"bootstrap-frontend-$playV"            % "8.4.0",
    "uk.gov.hmrc"          %% s"play-frontend-hmrc-$playV"            % "8.4.0",
    "uk.gov.hmrc"          %% s"play-conditional-form-mapping-$playV" % "2.0.0",
    "uk.gov.hmrc"          %% s"crypto-json-$playV"                   % "7.6.0",
    "com.vladsch.flexmark"  % "flexmark-all"                          % "0.64.8",
    "com.beachape"         %% "enumeratum-play"                       % "1.8.0",
    "com.squareup.okhttp3"  % "mockwebserver"                         % "3.9.1",
    "org.playframework"    %% "play-json"                             % playJsonV,
    "org.playframework"    %% "play-json-joda"                        % playJsonV,
    "org.typelevel"        %% "cats-core"                             % "2.10.0",
    "commons-codec"         % "commons-codec"                         % "1.15"
  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }

  object Test {
    def apply() = new ScopeDependencies {
      override val scope = "test"
      override lazy val dependencies = Seq(
        "org.scalacheck"         %% "scalacheck"         % "1.14.3"  % scope,
        "org.pegdown"             % "pegdown"            % "1.6.0"   % scope,
        "org.jsoup"               % "jsoup"              % "1.13.1"  % scope,
        "org.playframework"      %% "play-test"          % "3.0.1"   % scope,
        "org.mockito"             % "mockito-all"        % "1.10.19" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0"   % scope
      )
    }.dependencies
  }

  def apply() = compile ++ Test()
}
