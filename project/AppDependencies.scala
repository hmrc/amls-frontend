import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.*

private object AppDependencies {

  private val playV = "play-28"
  private val flexmarkVersion = "0.64.8"
  private val bootstrapV = "6.0.0"
  private val hmrcMongoV = "0.71.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    // GOV UK
    "uk.gov.hmrc"           %% "domain"                        % s"8.1.0-$playV",
    "uk.gov.hmrc"           %% "play-partials"                 % s"8.3.0-$playV",
    "uk.gov.hmrc"           %% "http-caching-client"           % s"10.0.0-$playV",
    "uk.gov.hmrc"           %% "json-encryption"               % s"5.3.0-$playV",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playV"            % hmrcMongoV,
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playV"    % bootstrapV,
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playV"    % "8.5.0",
    "uk.gov.hmrc"           %% "play-conditional-form-mapping" % s"1.13.0-$playV",
    // OTHER
    "com.vladsch.flexmark"   % "flexmark-all"                  % flexmarkVersion,
    "com.beachape"          %% "enumeratum-play"               % "1.6.3",
    "com.squareup.okhttp3"   % "mockwebserver"                 % "4.12.0",
    "org.typelevel"         %% "cats-core"                     % "2.10.0",
    "commons-codec"          % "commons-codec"                 % "1.15",
    // PLAY
    "com.typesafe.play"     %% "play-json"                     % "2.8.1",
    "com.typesafe.play"     %% "play-json-joda"                % "2.8.1"
  )

  trait ScopeDependencies {
    val scope: String
    val dependencies: Seq[ModuleID]
  }

  object Test {
    def apply(): Seq[sbt.ModuleID] = new ScopeDependencies {
      override val scope = "test,it"
      override lazy val dependencies: Seq[sbt.ModuleID] = Seq(
        "uk.gov.hmrc"            %% s"bootstrap-test-$playV"  % bootstrapV          % scope,
        "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playV" % hmrcMongoV          % scope,
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

  def apply(): Seq[ModuleID] = compile ++ Test()
}
