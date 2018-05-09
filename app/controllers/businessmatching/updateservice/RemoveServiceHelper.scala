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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.RemoveServiceFlowModel
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class RemoveServiceHelper @Inject()(val authConnector: AuthConnector,
                                    implicit val dataCacheConnector: DataCacheConnector
                                   ) {


  def removeBusinessActivities(model: RemoveServiceFlowModel)(implicit ac: AuthContext, hc: HeaderCarrier): OptionT[Future, BusinessMatching] = {
  ???
  }

}
