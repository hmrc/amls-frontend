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

package controllers.businessmatching.updateservice

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import models.asp.Asp
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import play.api.i18n.MessagesApi
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.new_service_information
import play.api.mvc.Result
import uk.gov.hmrc.http.cache.client.CacheMap
import models.moneyservicebusiness.{MoneyServiceBusiness => MSBModel}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import models.hvd.Hvd
import models.tcsp.Tcsp

class NewServiceInformationController @Inject()(
                                       val authConnector: AuthConnector,
                                       val dataCacheConnector: DataCacheConnector,
                                       val statusService: StatusService,
                                       val businessMatchingService: BusinessMatchingService,
                                       val messages: MessagesApi) extends BaseController {



  def get() = Authorised.async {
    implicit request =>
      implicit authContext => {
        for {
          activities <- businessMatchingService.getAdditionalBusinessActivities
        } yield Ok(new_service_information(activities.head))
      } getOrElse InternalServerError("Unable to get business activities")
  }

  private val activityToUrl = Map[BusinessActivity, String](
    MoneyServiceBusiness -> controllers.msb.routes.WhatYouNeedController.get().url,
    HighValueDealing -> controllers.hvd.routes.WhatYouNeedController.get().url,
    TrustAndCompanyServices -> controllers.tcsp.routes.WhatYouNeedController.get().url,
    EstateAgentBusinessService -> controllers.estateagentbusiness.routes.WhatYouNeedController.get().url,
    AccountancyServices -> controllers.asp.routes.WhatYouNeedController.get().url
  )

  private val activityToData = Map[BusinessActivity, CacheMap => Boolean](
    MoneyServiceBusiness -> { c => c.getEntry[MSBModel](MSBModel.key).fold(false)(_.isComplete(true, true)) },
    HighValueDealing -> { c => c.getEntry[Hvd](Hvd.key).fold(false)(_.isComplete) },
    TrustAndCompanyServices -> { c => c.getEntry[Tcsp](Tcsp.key).fold(false)(_.isComplete) },
    EstateAgentBusinessService -> { c => c.getEntry[EstateAgentBusiness](EstateAgentBusiness.key).fold(false)(_.isComplete) },
    AccountancyServices -> { c => c.getEntry[Asp](Asp.key).fold(false)(_.isComplete) }
  )

  private[controllers] def getNextFlow(implicit hc: HeaderCarrier, ac: AuthContext) = {
    for {
      cacheMap <- OptionT(dataCacheConnector.fetchAll)
      activities <- businessMatchingService.getAdditionalBusinessActivities
    } yield (activities collectFirst {
      case act if !activityToData(act)(cacheMap) => Redirect(activityToUrl(act))
    }).get
  }

  def post() = Authorised.async {
    implicit request =>
      implicit authContext =>
        ???
  }

}
