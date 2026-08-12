import play.sbt.PlayImport.ws
import sbt.*

private object AppDependencies {

  private val playV = "play-30"
  private val flexmarkVersion = "0.64.8"
  private val bootstrapV = "10.8.0"
  private val hmrcMongoV = "2.13.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    // GOV UK
    "uk.gov.hmrc"           %% s"domain-$playV"                        % "13.0.0",
    "uk.gov.hmrc"           %% s"play-partials-$playV"                 % "10.2.0",
    "uk.gov.hmrc"           %% s"crypto-json-$playV"                   % "8.4.0",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playV"                    % hmrcMongoV,
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playV"            % bootstrapV,
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playV"            % "13.10.0",
    "uk.gov.hmrc"           %% s"play-conditional-form-mapping-$playV" % "3.5.0",
    // OTHER
    "com.vladsch.flexmark"   % "flexmark"                              % flexmarkVersion exclude("org.apache.pdfbox", "pdfbox"),
    "com.beachape"          %% "enumeratum-play"                       % "1.9.8",
    "org.typelevel"         %% "cats-core"                             % "2.13.0",
    "commons-codec"          % "commons-codec"                         % "1.22.1"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playV"  % bootstrapV % Test,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playV" % hmrcMongoV % Test,
    "org.scalatestplus"      %% "scalacheck-1-17"         % "3.2.18.0" % Test,
    "org.scalamock"          %% "scalamock"               % "6.0.0"    % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
