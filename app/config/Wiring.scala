package config

import com.typesafe.config.Config
import play.api.Play
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.config.{LoadAuditingConfig, AuditingConfig}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{ControllerConfig, AppName, RunMode}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import uk.gov.hmrc.play.http.ws.{WSGet, WSPut, WSPost, WSDelete}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartial

object AMLSControllerConfig extends ControllerConfig {
  override def controllerConfigs: Config = Play.current.configuration.underlying.getConfig("controllers")
}

object AMLSAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"$env.auditing")
}

object AMLSAuthConnector extends AuthConnector {
  override val serviceUrl: String = ApplicationConfig.authHost
  override lazy val http: HttpGet = WSHttp
}

object AMLSAuditFilter extends FrontendAuditFilter with AppName {

  override lazy val maskedFormFields: Seq[String] = Nil

  override lazy val applicationPort: Option[Int] = None

  override lazy val auditConnector: AuditConnector = AMLSAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    AMLSControllerConfig.paramsForController(controllerName).needsAuditing
}

object AMLSLoggingFilter extends FrontendLoggingFilter {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    AMLSControllerConfig.paramsForController(controllerName).needsLogging
}

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode {
  override lazy val auditConnector: AuditConnector = AMLSAuditConnector
}

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartial {
  override lazy val httpGet: HttpGet = WSHttp
}
