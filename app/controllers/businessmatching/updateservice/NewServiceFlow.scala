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

import models.businessmatching._
import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector

import scala.concurrent.Future
import models.hvd.Hvd
import models.tcsp.Tcsp
import models.estateagentbusiness.EstateAgentBusiness
import models.asp.Asp
import uk.gov.hmrc.http.cache.client.CacheMap
import models.moneyservicebusiness.{MoneyServiceBusiness => MSBModel}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext
import services.businessmatching.BusinessMatchingService

case class NextService(url: String, activity: BusinessActivity)

trait NewServiceFlow {


  val businessMatchingService: BusinessMatchingService

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

  private[controllers] def getNextFlow(implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext, cacheMap: CacheMap) = {

    def redirectUrl(activities: Set[BusinessActivity], cacheMap: CacheMap) = OptionT.fromOption[Future](
      activities collectFirst {
        case act if !activityToData(act)(cacheMap) => (activityToUrl(act), act)
      }
    )

    for {
      activities <- businessMatchingService.getAdditionalBusinessActivities
      (url, activity) <- redirectUrl(activities, cacheMap)
    } yield NextService(url, activity)
  }
}
