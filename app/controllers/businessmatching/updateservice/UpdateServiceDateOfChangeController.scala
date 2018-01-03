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

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.routes._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.businessmatching._
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tradingpremises.TradingPremises
import play.api.mvc.{Request, Result}
import services.TradingPremisesService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UpdateServiceDateOfChangeController @Inject()(
                                                   val authConnector: AuthConnector,
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val businessMatchingService: BusinessMatchingService,
                                                   val tradingPremisesService: TradingPremisesService
                                                   ) extends BaseController with RepeatingSection {

  def get(services: String) = Authorised.async{
    implicit authContext =>
      implicit request =>
        mapRequestToActivities(services) match {
          case Right(_) => Future.successful(Ok(view(EmptyForm, services)))
          case Left(badRequest) => Future.successful(badRequest)
        }
  }

  private def mapRequestToActivities(services: String): Either[Result, Set[BusinessActivity]] =
    try {
      Right((services split "/" map BusinessActivities.getBusinessActivity).toSet)
    } catch {
      case _: MatchError => Left(BadRequest)
    }

  def post(activitiesInRequest: String) = Authorised.async{
    implicit authContext =>
      implicit request =>
        mapRequestToActivities(activitiesInRequest) match {
          case Right(activitiesToRemove) => Form2[DateOfChange](request.body) match {
            case ValidForm(_, data) =>
              (for {
                businessMatching <- businessMatchingService.getModel
                existingActivities <- OptionT.fromOption[Future](businessMatching.activities)
                updatedBusinessActivities <- OptionT.pure[Future, BusinessActivities](
                  existingActivities.copy(
                    businessActivities = existingActivities.businessActivities diff activitiesToRemove,
                    additionalActivities = existingActivities.additionalActivities,
                    removeActivities = existingActivities.removeActivities.fold[Option[Set[BusinessActivity]]](Some(activitiesToRemove)){ act =>
                      Some(act ++ activitiesToRemove)
                    },
                    dateOfChange = Some(data)
                  ))
                _ <- businessMatchingService.updateModel(businessMatching.activities(updatedBusinessActivities).copy(hasAccepted = true))
                _ <- OptionT.liftF(updateDataStrict[TradingPremises] { tradingPremises: Seq[TradingPremises] =>
                  tradingPremisesService.removeBusinessActivitiesFromTradingPremises(
                    tradingPremises,
                    existingActivities.businessActivities diff activitiesToRemove,
                    activitiesToRemove
                  )
                })
                _ <- businessMatchingService.commitVariationData
                _ <- OptionT.liftF(removeSection(activitiesToRemove))
                responsiblePeople <- OptionT.liftF(getData[ResponsiblePeople])
                fitAndProperRequired <- OptionT.pure[Future, Boolean](fitAndProperRequired(updatedBusinessActivities))
                responsiblePeopleWithoutFitAndProper <- OptionT.pure[Future, Seq[ResponsiblePeople]](withoutFitAndProper(responsiblePeople))
                _ <- maybeRemoveFitAndProper(responsiblePeopleWithoutFitAndProper, fitAndProperRequired)
              } yield Redirect(UpdateAnyInformationController.get())) getOrElse InternalServerError("Cannot remove business activities")
            case f:InvalidForm => Future.successful(BadRequest(view(f, activitiesInRequest)))
          }
          case Left(badRequest) => Future.successful(badRequest)
        }
  }

  private def removeSection(activities: Set[BusinessActivity])
                           (implicit hc: HeaderCarrier, ac: AuthContext): Future[Set[CacheMap]] = {

    def removeSupervision(activities: Set[BusinessActivity]): Boolean =
      activities exists { activity =>
        (activity equals AccountancyServices) | (activity equals TrustAndCompanyServices)
      }

    val withoutSection: PartialFunction[BusinessActivity, Boolean] = {
      case TelephonePaymentService | BillPaymentServices => false
      case _ => true
    }

    Future.sequence({
      activities filter withoutSection map businessMatchingService.clearSection
    } map { cache =>
      if(removeSupervision(activities)){
        dataCacheConnector.save[Supervision](Supervision.key, None)
      } else {
        cache
      }
    })
  }

  private def fitAndProperRequired(businessActivities: BusinessActivities): Boolean =
    (businessActivities.businessActivities contains MoneyServiceBusiness) | (businessActivities.businessActivities contains TrustAndCompanyServices)

  private def removeFitAndProper(responsiblePeople: Seq[ResponsiblePeople])(implicit ac: AuthContext, hc: HeaderCarrier): Future[Seq[ResponsiblePeople]] = {
    dataCacheConnector.save[Seq[ResponsiblePeople]](ResponsiblePeople.key, responsiblePeople) map { _ =>
      responsiblePeople
    }
  }

  private def withoutFitAndProper(responsiblePeople: Seq[ResponsiblePeople]) = responsiblePeople map { rp =>
    rp.hasAlreadyPassedFitAndProper(None).copy(hasAccepted = true)
  }

  private def maybeRemoveFitAndProper(responsiblePeople: Seq[ResponsiblePeople], fitAndProperRequired: Boolean)(implicit ac: AuthContext, hc: HeaderCarrier) =
    if(fitAndProperRequired){
      OptionT.pure[Future, Seq[ResponsiblePeople]](responsiblePeople)
    } else {
      OptionT.liftF(removeFitAndProper(responsiblePeople))
    }

  private def view(f: Form2[_], services: String)(implicit request: Request[_]) =
    views.html.date_of_change(f, "summary.updateservice", UpdateServiceDateOfChangeController.post(services))

}
