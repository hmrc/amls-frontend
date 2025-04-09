import play.sbt.PlayImport.ws
import sbt.*

private object AppDependencies {

  private val playV = "play-30"
  private val flexmarkVersion = "0.64.8"
  private val bootstrapV = "9.6.0"
  private val hmrcMongoV = "2.6.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    // GOV UK
    "uk.gov.hmrc"           %% s"domain-$playV"                        % "10.0.0",
    "uk.gov.hmrc"           %% s"play-partials-$playV"                 % "10.0.0",
    "uk.gov.hmrc"           %% s"crypto-json-$playV"                   % "8.2.0",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playV"                    % hmrcMongoV,
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playV"            % bootstrapV,
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playV"            % "11.13.0",
    "uk.gov.hmrc"           %% s"play-conditional-form-mapping-$playV" % "3.3.0",
    // OTHER
    "com.vladsch.flexmark"   % "flexmark"                              % flexmarkVersion exclude("org.apache.pdfbox", "pdfbox"),
    "com.beachape"          %% "enumeratum-play"                       % "1.8.2",
    "org.typelevel"         %% "cats-core"                             % "2.12.0",
    "commons-codec"          % "commons-codec"                         % "1.17.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playV"  % bootstrapV % Test,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playV" % hmrcMongoV % Test,
    "org.scalatestplus"      %% "scalacheck-1-17"         % "3.2.18.0" % Test,
    "org.scalamock"          %% "scalamock"               % "5.2.0"    % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
