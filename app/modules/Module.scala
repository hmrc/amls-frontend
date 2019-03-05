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

package modules

import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.typesafe.config.Config
import config.{AMLSAuditConnector, AMLSAuditFilter, AMLSLoggingFilter, WSHttp}
import models.businessmatching.updateservice.ChangeBusinessType
import models.flowmanagement.{AddBusinessTypeFlowModel, ChangeSubSectorFlowModel, RemoveBusinessTypeFlowModel}
import play.api.Configuration
import services.flowmanagement.Router
import services.flowmanagement.flowrouters.businessmatching.{AddBusinessTypeRouter, ChangeBusinessTypeRouter, ChangeSubSectorRouter, RemoveBusinessTypeRouter}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter}

class Module extends AbstractModule {

  type HmrcAuthConnector = uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

  def configure() = {
    bind(classOf[HttpGet]).to(classOf[WSHttp])
    bind(classOf[HttpPost]).to(classOf[WSHttp])
    bind(classOf[HttpDelete]).to(classOf[WSHttp])
    bind(classOf[HmrcAuthConnector]).to(classOf[config.FrontendAuthConnector])
    bind(classOf[AuditConnector]).to(classOf[AMLSAuditConnector])
    bind(classOf[CorePost]).to(classOf[WSHttp])
    bind(classOf[CoreGet]).to(classOf[WSHttp])
    bind(classOf[FrontendLoggingFilter]).to(classOf[AMLSLoggingFilter])
    bind(classOf[FrontendAuditFilter]).to(classOf[AMLSAuditFilter])
    bind(new TypeLiteral[Router[AddBusinessTypeFlowModel]] {}).to(classOf[AddBusinessTypeRouter])
    bind(new TypeLiteral[Router[ChangeBusinessType]] {}).to(classOf[ChangeBusinessTypeRouter])
    bind(new TypeLiteral[Router[RemoveBusinessTypeFlowModel]] {}).to(classOf[RemoveBusinessTypeRouter])
    bind(new TypeLiteral[Router[ChangeSubSectorFlowModel]] {}).to(classOf[ChangeSubSectorRouter])
  }

  @Provides
  def configProvider(configuration: Configuration): Config =
    configuration.underlying
}
