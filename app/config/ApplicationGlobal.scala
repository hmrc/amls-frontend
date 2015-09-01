package config

import play.api.mvc.Request
import play.api.{Configuration, Application}
import play.twirl.api.{HtmlFormat, Html}
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

object ApplicationGlobal extends DefaultFrontendGlobal {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.error(pageTitle, heading, message)

  override lazy val auditConnector: AuditConnector = AMLSAuditConnector

  override lazy val loggingFilter: FrontendLoggingFilter = AMLSLoggingFilter

  override lazy val frontendAuditFilter: FrontendAuditFilter = AMLSAuditFilter

  // TODO: Create a metrics configuration
  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = None
}
