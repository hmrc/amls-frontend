import play.sbt.PlayImport.ws
import sbt.*

private object AppDependencies {

  private val playV = "play-30"
  private val flexmarkVersion = "0.64.8"
  private val bootstrapV = "8.5.0"
  private val hmrcMongoV = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    // GOV UK
    "uk.gov.hmrc"           %% s"domain-$playV"                        % "9.0.0",
    "uk.gov.hmrc"           %% s"play-partials-$playV"                 % "9.1.0",
    "uk.gov.hmrc"           %% s"crypto-json-$playV"                   % "7.6.0",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playV"                    % hmrcMongoV,
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playV"            % bootstrapV,
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playV"            % "9.10.0",
    "uk.gov.hmrc"           %% s"play-conditional-form-mapping-$playV" % "2.0.0",
    // OTHER
    "com.vladsch.flexmark"   % "flexmark-all"                          % flexmarkVersion,
    "com.beachape"          %% "enumeratum-play"                       % "1.8.0",
    "org.typelevel"         %% "cats-core"                             % "2.12.0",
    "commons-codec"          % "commons-codec"                         % "1.15"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playV"  % bootstrapV % Test,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playV" % hmrcMongoV % Test,
    "org.scalatestplus"      %% "scalacheck-1-17"         % "3.2.17.0" % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
