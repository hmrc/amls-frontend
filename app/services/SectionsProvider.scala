/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.DataCacheConnector
import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.registrationprogress.{Section, TaskRow}
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SectionsProvider @Inject()(protected val cacheConnector: DataCacheConnector,
                                 val config: ApplicationConfig) {

  def sections(cacheId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[Section]] =

    cacheConnector.fetchAll(cacheId) map {
      optionCache =>
        optionCache map {
          cache =>
            sections(cache)
        } getOrElse Seq.empty
    }

  def sections(cache: CacheMap) : Seq[Section] = {
    mandatorySections(cache) ++ dependentSections(cache)
  }

  def taskRows(cacheId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Seq[TaskRow]] =
    cacheConnector.fetchAll(cacheId) map {
      optionCache =>
        optionCache map {
          cache =>
            taskRows(cache)
        } getOrElse Seq.empty
    }

  def taskRows(cache: CacheMap)(implicit messages: Messages): Seq[TaskRow] = {
    mandatoryTaskRows(cache, messages) ++ dependentTaskRows(cache, messages)
  }

  def sectionsFromBusinessActivities(activities: Set[BusinessActivity],
                                     msbServices: Option[BusinessMatchingMsbServices])
                                    (implicit cache: CacheMap): Seq[Section] = {

    val asp = if (activities.contains(AccountancyServices)) Seq(Asp.section) else Seq.empty
    val tcsp = if (activities.contains(TrustAndCompanyServices)) Seq(Tcsp.section) else Seq.empty
    val supervision = if (asp.nonEmpty || tcsp.nonEmpty) Seq(Supervision.section) else Seq.empty
    val amp = if (activities.contains(ArtMarketParticipant)) Seq(Amp.section(config)) else Seq.empty
    val eab = if (activities.contains(EstateAgentBusinessService)) Seq(Eab.section(config)) else Seq.empty
    val hvd = if (activities.contains(HighValueDealing)) Seq(Hvd.section) else Seq.empty
    val msb = if (activities.contains(MoneyServiceBusiness) && msbServices.isDefined) Seq(Msb.section) else Seq.empty

    asp ++ tcsp ++ supervision ++ amp ++ eab ++ hvd ++ msb
  }

  def taskRowsFromBusinessActivities(activities: Set[BusinessActivity],
                                     msbServices: Option[BusinessMatchingMsbServices])
                                    (implicit cache: CacheMap, messages: Messages): Seq[TaskRow] = {

    val asp = if (activities.contains(AccountancyServices)) Seq(Asp.taskRow) else Seq.empty
    val tcsp = if (activities.contains(TrustAndCompanyServices)) Seq(Tcsp.taskRow) else Seq.empty
    val supervision = if (asp.nonEmpty || tcsp.nonEmpty) Seq(Supervision.taskRow) else Seq.empty
    val amp = if (activities.contains(ArtMarketParticipant)) Seq(Amp.taskRow(config)) else Seq.empty
    val eab = if (activities.contains(EstateAgentBusinessService)) Seq(Eab.taskRow(config)) else Seq.empty
    val hvd = if (activities.contains(HighValueDealing)) Seq(Hvd.taskRow) else Seq.empty
    val msb = if (activities.contains(MoneyServiceBusiness) && msbServices.isDefined) Seq(Msb.taskRow) else Seq.empty

    asp ++ tcsp ++ supervision ++ amp ++ eab ++ hvd ++ msb
  }

  private def dependentSections(implicit cache: CacheMap): Seq[Section] =
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      ba <- bm.activities
    } yield sectionsFromBusinessActivities(ba.businessActivities, bm.msbServices)) getOrElse Seq.empty

  private def dependentTaskRows(implicit cache: CacheMap, messages: Messages): Seq[TaskRow] =
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      ba <- bm.activities
    } yield taskRowsFromBusinessActivities(ba.businessActivities, bm.msbServices)) getOrElse Seq.empty

  private def mandatorySections(implicit cache: CacheMap): Seq[Section] =
    Seq(
      BusinessMatching.section,
      BusinessDetails.section,
      BusinessActivities.section,
      BankDetails.section,
      TradingPremises.section,
      ResponsiblePerson.section
    )

  private def mandatoryTaskRows(implicit cache: CacheMap, messages: Messages): Seq[TaskRow] =
    Seq(
      BusinessMatching.taskRow,
      BusinessDetails.taskRow,
      BusinessActivities.taskRow,
      BankDetails.taskRow,
      TradingPremises.taskRow,
      ResponsiblePerson.taskRow
    )
}
