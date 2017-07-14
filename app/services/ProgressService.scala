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

package services

import cats.data.OptionT
import config.ApplicationConfig
import connectors.DataCacheConnector
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessType.Partnership
import models.businessmatching.{BusinessActivities => _, _}
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.estateagentbusiness.EstateAgentBusiness
import models.registrationprogress.{NotStarted, Section}
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{ControllerHelper, DeclarationHelper}
import play.api.mvc.{Action, AnyContent, Call, Request}

import scala.concurrent.{ExecutionContext, Future}

trait ProgressService {

  private[services] def cacheConnector: DataCacheConnector
  private[services] def statusService: StatusService

  private def dependentSections(implicit cache: CacheMap): Set[Section] =
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      ba <- bm.activities
    } yield ba.businessActivities.foldLeft[Set[Section]](Set.empty) {
      (m, n) => n match {
        case AccountancyServices =>
          m + Asp.section + Supervision.section
        case EstateAgentBusinessService =>
          m + EstateAgentBusiness.section
        case HighValueDealing =>
          m + Hvd.section
        case MoneyServiceBusiness =>
          bm.msbServices.fold(m)(x => m + Msb.section)
        case TrustAndCompanyServices =>
          m + Tcsp.section + Supervision.section
        case _ => m
      }
        // TODO Error instead of empty map
    }) getOrElse Set.empty

  private def mandatorySections(implicit cache: CacheMap): Seq[Section] =
    Seq(
      BusinessMatching.section,
      AboutTheBusiness.section,
      BusinessActivities.section,
      BankDetails.section,
      TradingPremises.section,
      ResponsiblePeople.section
    )


  def sections
  (implicit
   hc: HeaderCarrier,
   ac: AuthContext,
   ec: ExecutionContext
  ): Future[Seq[Section]] =
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

  /*def getSubmitRedirect (implicit auth: AuthContext, request: Request[AnyContent]) : Future[Call] = {

    val result = for {
      status <- OptionT.liftF(statusService.getStatus)
      responsiblePeople <- OptionT(cacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
      hasNominatedOfficer <- OptionT.liftF(ControllerHelper.hasNominatedOfficer(Future.successful(Some(responsiblePeople))))
      businessmatching <- OptionT(cacheConnector.fetch[BusinessMatching](BusinessMatching.key))
      reviewDetails <- OptionT.fromOption[Future](businessmatching.reviewDetails)
      businessType <- OptionT.fromOption[Future](reviewDetails.businessType)
    } yield {

      businessType match {
        case Partnership if DeclarationHelper.numberOfPartners(responsiblePeople) < 2 => {
          controllers.declaration.routes.RegisterPartnersController.get()
        }
        case _ => DeclarationHelper.routeDependingOnNominatedOfficer(hasNominatedOfficer, status)
      }

    }
    result getOrElse controllers.routes.RegistrationProgressController.get()
  }*/
}

object ProgressService extends ProgressService {
  override private[services] val cacheConnector = DataCacheConnector
  override private[services] val statusService = StatusService
}
