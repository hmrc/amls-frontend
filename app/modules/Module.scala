/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.{AbstractModule, TypeLiteral}
import config.{AMLSAuditConnector, WSHttp}
import connectors._
import connectors.cache.{AmlsMongoCacheConnector, CacheConnector, Save4LaterCacheConnector}
import models.businessmatching.updateservice.ChangeBusinessType
import models.flowmanagement.{AddBusinessTypeFlowModel, ChangeSubSectorFlowModel, RemoveBusinessTypeFlowModel}
import services._
import services.flowmanagement.Router
import services.flowmanagement.flowrouters.businessmatching.{AddBusinessTypeRouter, ChangeBusinessTypeRouter, ChangeSubSectorRouter, RemoveBusinessTypeRouter}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class Module extends AbstractModule {

  type HmrcAuthConnector = uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

  override def configure() = {
    bind(classOf[HttpGet]).toInstance(WSHttp)
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[HttpDelete]).toInstance(WSHttp)
    bind(classOf[WSHttp]).toInstance(WSHttp)
    bind(classOf[KeystoreConnector]).toInstance(KeystoreConnector)
    bind(classOf[DataCacheConnector]).toInstance(DataCacheConnector)
    bind(classOf[CacheConnector]).to(classOf[Save4LaterCacheConnector])
    bind(classOf[HmrcAuthConnector]).to(classOf[config.FrontendAuthConnector])
    bind(classOf[AmlsNotificationConnector]).toInstance(AmlsNotificationConnector)
    bind(classOf[StatusService]).toInstance(StatusService)
    bind(classOf[AmlsConnector]).toInstance(AmlsConnector)
    bind(classOf[AuditConnector]).toInstance(AMLSAuditConnector)
    bind(classOf[GovernmentGatewayService]).toInstance(GovernmentGatewayService)
    bind(classOf[CorePost]).toInstance(WSHttp)
    bind(classOf[CoreGet]).toInstance(WSHttp)
    bind(classOf[LandingService]).toInstance(LandingService)
    bind(new TypeLiteral[Router[AddBusinessTypeFlowModel]] {}).to(classOf[AddBusinessTypeRouter])
    bind(new TypeLiteral[Router[ChangeBusinessType]] {}).to(classOf[ChangeBusinessTypeRouter])
    bind(new TypeLiteral[Router[RemoveBusinessTypeFlowModel]] {}).to(classOf[RemoveBusinessTypeRouter])
    bind(new TypeLiteral[Router[ChangeSubSectorFlowModel]] {}).to(classOf[ChangeSubSectorRouter])
  }
}
