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

package services

import connectors.DataCacheConnector
import javax.inject.Inject
import models.businessdetails.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.registrationprogress.Section
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class SectionsProvider @Inject()(protected val cacheConnector: DataCacheConnector) {
  def sections
  (implicit hc: HeaderCarrier,
            ac: AuthContext,
            ec: ExecutionContext): Future[Seq[Section]] =

    cacheConnector.fetchAll map {
      optionCache =>
        optionCache map {
          cache =>
            sections(cache)
        } getOrElse Seq.empty
    }

  def sections(cache : CacheMap) : Seq[Section] = {
      mandatorySections(cache) ++
      dependentSections(cache)
  }

  def sectionsFromBusinessActivities(activities: Set[BusinessActivity],
                                     msbServices: Option[BusinessMatchingMsbServices]
                                    )(implicit cache: CacheMap): Set[Section] =
    activities.foldLeft[Set[Section]](Set.empty) {
      (m, n) => n match {
        case AccountancyServices =>
          m + Asp.section + Supervision.section
        case EstateAgentBusinessService =>
          m + EstateAgentBusiness.section
        case HighValueDealing =>
          m + Hvd.section
        case MoneyServiceBusiness if msbServices.isDefined =>
          m + Msb.section
        case TrustAndCompanyServices =>
          m + Tcsp.section + Supervision.section
        case _ => m
      }
    }

  private def dependentSections(implicit cache: CacheMap): Set[Section] =
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      ba <- bm.activities
    } yield sectionsFromBusinessActivities(ba.businessActivities, bm.msbServices)) getOrElse Set.empty

  private def mandatorySections(implicit cache: CacheMap): Seq[Section] =
    Seq(
      BusinessMatching.section,
      AboutTheBusiness.section,
      BusinessActivities.section,
      BankDetails.section,
      TradingPremises.section,
      ResponsiblePerson.section
    )

}
