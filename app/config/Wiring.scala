/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import com.typesafe.config.Config
import play.api.Mode.Mode
import play.api.{Application, Configuration, Environment, Play}
import play.api.mvc.Call
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.partials.CachedStaticHtmlPartialRetriever
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq.empty
}

class WSHttp @Inject()( application: Application,
                        amlsAuditConnector: AMLSAuditConnector,
                        val actorSystem: ActorSystem) extends HttpGet
  with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete  with WSDelete
  with Hooks with HttpPatch with WSPatch with AppName with RunMode {

  override lazy val auditConnector: AuditConnector = amlsAuditConnector
  override protected def appNameConfiguration: Configuration = application.configuration
  override protected def configuration: Option[Config] = Some(application.configuration.underlying)
  override protected def mode: Mode = application.mode
  override protected def runModeConfiguration: Configuration = application.configuration
}

// TODO: Maintaining a WSHttp object for static references in twirl templates. Remove when upgrading to 2.6
object StaticWSHttp  extends HttpGet
  with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete  with WSDelete
  with Hooks with HttpPatch with WSPatch with AppName with RunMode {

  lazy val wsHttp: WSHttp = new WSHttp(Play.current, new AMLSAuditConnector(Play.current), Play.current.actorSystem)
  override lazy val auditConnector: AuditConnector = wsHttp.auditConnector
  override protected def appNameConfiguration: Configuration = Play.current.configuration
  override protected def configuration: Option[Config] = Some(Play.current.configuration.underlying)
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  protected def actorSystem: ActorSystem = Play.current.actorSystem
}

class AMLSControllerConfig @Inject()(application: Application) extends ControllerConfig {
  override def controllerConfigs: Config = application.configuration.underlying.getConfig("controllers")
}

class BusinessCustomerSessionCache @Inject()(application: Application, wsHttp: WSHttp) extends SessionCache with AppName with ServicesConfig{
  override lazy val http = wsHttp
  override lazy val defaultSource: String = getConfString("cachable.session-cache.review-details.cache","business-customer-frontend")

  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))

  override protected def appNameConfiguration: Configuration = application.configuration
  override protected def mode: Mode = application.mode
  override protected def runModeConfiguration: Configuration = application.configuration
}

@Singleton
class FrontendAuthConnector @Inject()( environment: Environment,
                                       val runModeConfiguration: Configuration,
                                       wsHttp: WSHttp) extends AuthConnector with ServicesConfig {
  lazy val serviceUrl = baseUrl("auth")
  override def http = wsHttp
  override protected def mode: Mode = environment.mode
}

class AmlsSessionCache @Inject()( environment: Environment,
                                  override val runModeConfiguration: Configuration,
                                  override val appNameConfiguration: Configuration, wsHttp: WSHttp)
  extends SessionCache with AppName with ServicesConfig {

  override def http = wsHttp
  override def defaultSource = getConfString("amls-frontend.cache", "amls-frontend")
  override def baseUri = baseUrl("cachable.session-cache")
  override def domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
  override protected def mode: Mode = environment.mode
}

class AMLSAuditConnector @Inject()( application: Application ) extends AuditConnector with RunMode {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")
  override protected def mode: Mode = application.mode
  override protected def runModeConfiguration: Configuration = application.configuration
}

class AMLSAuthConnector @Inject()(wsHttp: WSHttp) extends AuthConnector {
  override val serviceUrl: String = ApplicationConfig.authHost
  override lazy val http: HttpGet = wsHttp
}

class AMLSAuditFilter @Inject()(
                     application: Application,
                     amlsControllerConfig: AMLSControllerConfig,
                     amlsAuditConnector: AMLSAuditConnector) extends FrontendAuditFilter with AppName with MicroserviceFilterSupport{

  override lazy val maskedFormFields: Seq[String] = Nil

  override lazy val applicationPort: Option[Int] = None

  override lazy val auditConnector: AuditConnector = amlsAuditConnector

  override def controllerNeedsAuditing(controllerName: String): Boolean =
    amlsControllerConfig.paramsForController(controllerName).needsAuditing

  override protected def appNameConfiguration: Configuration = application.configuration
}

class AMLSLoggingFilter @Inject()( application: Application,
                                   amlsControllerConfig: AMLSControllerConfig) extends FrontendLoggingFilter with MicroserviceFilterSupport{
  override def controllerNeedsLogging(controllerName: String): Boolean =
    amlsControllerConfig.paramsForController(controllerName).needsLogging
}

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartialRetriever {
  override lazy val httpGet: HttpGet = StaticWSHttp
}

object WhitelistFilter extends AkamaiWhitelistFilter with MicroserviceFilterSupport{
  override def whitelist: Seq[String] = ApplicationConfig.whitelist
  override def destination: Call = Call("GET", "https://www.gov.uk")
}
