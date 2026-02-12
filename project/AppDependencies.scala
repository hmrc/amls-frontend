import play.sbt.PlayImport.ws
import sbt.*

private object AppDependencies {

  private val playV = "play-30"
  private val flexmarkVersion = "0.64.8"
  private val bootstrapV = "10.5.0"
  private val hmrcMongoV = "2.12.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    // GOV UK
    "uk.gov.hmrc"           %% s"domain-$playV"                        % "10.0.0",
    "uk.gov.hmrc"           %% s"play-partials-$playV"                 % "10.2.0",
    "uk.gov.hmrc"           %% s"crypto-json-$playV"                   % "8.4.0",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$playV"                    % hmrcMongoV,
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$playV"            % bootstrapV exclude("org.apache.commons", "commons-lang3"),
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$playV"            % "12.25.0",
    "uk.gov.hmrc"           %% s"play-conditional-form-mapping-$playV" % "3.4.0",
    // OTHER
    "com.vladsch.flexmark"   % "flexmark"                              % flexmarkVersion exclude("org.apache.pdfbox", "pdfbox"),
    "com.beachape"          %% "enumeratum-play"                       % "1.8.2",
    "org.typelevel"         %% "cats-core"                             % "2.12.0",
    "commons-codec"          % "commons-codec"                         % "1.17.2",
    "org.apache.commons"    % "commons-lang3"                          % "3.18.0",
    "ch.qos.logback"        % "logback-core"                           % "1.5.27",
    "ch.qos.logback"        % "logback-classic"                        % "1.5.27",
    "at.yawk.lz4"           %  "lz4-java"                              % "1.10.3"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playV"  % bootstrapV % Test,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playV" % hmrcMongoV % Test,
    "org.scalatestplus"      %% "scalacheck-1-17"         % "3.2.18.0" % Test,
    "org.scalamock"          %% "scalamock"               % "5.2.0"    % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
