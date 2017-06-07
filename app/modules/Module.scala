/*
 * Copyright 2017 HM Revenue & Customs
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

import com.google.inject.AbstractModule
import config.WSHttp
import connectors._
import services.{AuthEnrolmentsService, ProgressService, StatusService}
import uk.gov.hmrc.play.http.HttpPost

class Module extends AbstractModule {

  type HmrcAuthConnector = uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

  override def configure() = {
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[KeystoreConnector]).toInstance(KeystoreConnector)
    bind(classOf[DataCacheConnector]).toInstance(DataCacheConnector)
    bind(classOf[HmrcAuthConnector]).to(classOf[config.FrontendAuthConnector])
    bind(classOf[connectors.AuthConnector]).toInstance(AuthConnector)
    bind(classOf[ProgressService]).toInstance(ProgressService)
    bind(classOf[AmlsNotificationConnector]).toInstance(AmlsNotificationConnector)
    bind(classOf[StatusService]).toInstance(StatusService)
    bind(classOf[AuthEnrolmentsService]).toInstance(AuthEnrolmentsService)
    bind(classOf[AmlsConnector]).toInstance(AmlsConnector)
  }
}
