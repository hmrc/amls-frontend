package config

import com.typesafe.config.Config
import play.api.Play
import play.api.mvc.Call
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.cache.client.{SessionCache, ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.{LoadAuditingConfig, AuditingConfig}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{ServicesConfig, ControllerConfig, AppName, RunMode}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import uk.gov.hmrc.play.http.ws.{WSGet, WSPut, WSPost, WSDelete}
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

object AMLSControllerConfig extends ControllerConfig {
  override def controllerConfigs: Config = Play.current.configuration.underlying.getConfig("controllers")
}

object BusinessCustomerSessionCache extends SessionCache with AppName with ServicesConfig{
  override lazy val http = WSHttp
  override lazy val defaultSource: String = getConfString("cachable.session-cache.review-details.cache","business-customer-frontend")

  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}

object AMLSAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"$env.auditing")
}

object AMLSAuthConnector extends AuthConnector {
  override val serviceUrl: String = ApplicationConfig.authHost
  override lazy val http: HttpGet = WSHttp
}

object AMLSAuditFilter extends FrontendAuditFilter with AppName with MicroserviceFilterSupport{

  override lazy val maskedFormFields: Seq[String] = Nil

  override lazy val applicationPort: Option[Int] = None

  override lazy val auditConnector: AuditConnector = AMLSAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    AMLSControllerConfig.paramsForController(controllerName).needsAuditing
}

object AMLSLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport{
  override def controllerNeedsLogging(controllerName: String): Boolean =
    AMLSControllerConfig.paramsForController(controllerName).needsLogging
}

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode  with HttpAuditing {
  override lazy val auditConnector: AuditConnector = AMLSAuditConnector
  override val hooks = Seq(AuditingHook)
}

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartialRetriever {
  override lazy val httpGet: HttpGet = WSHttp
}

object AmlsShortLivedHttpCaching extends ShortLivedHttpCaching with AppName with ServicesConfig {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.short-lived-cache")
  override lazy val domain = getConfString("cachable.short-lived-cache.domain",
    throw new Exception(s"Could not find config 'cachable.short-lived-cache.domain'"))
}

object AmlsShortLivedCache extends ShortLivedCache {
  override implicit lazy val crypto = ApplicationCrypto.JsonCrypto
  override lazy val shortLiveCache = AmlsShortLivedHttpCaching
}

object WhitelistFilter extends AkamaiWhitelistFilter with MicroserviceFilterSupport{
  override def whitelist: Seq[String] = ApplicationConfig.whitelist
  // TODO redirect to shutter page when it is configured
  override def destination: Call = Call("GET", "https://www.gov.uk")
}
