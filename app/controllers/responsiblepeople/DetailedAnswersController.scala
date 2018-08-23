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

package controllers.responsiblepeople

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, AppConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.Partnership
import models.responsiblepeople.ResponsiblePerson
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, DeclarationHelper, RepeatingSection}
import models.responsiblepeople.ResponsiblePerson.{flowChangeOfficer, flowFromDeclaration}
import play.api.Play

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait DetailedAnswersController extends BaseController with RepeatingSection {

  protected def statusService: StatusService

  protected def config: AppConfig

  private def showHideAddressMove(lineId: Option[Int])(implicit authContext: AuthContext, headerCarrier: HeaderCarrier): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) if lineId.isDefined => true
      case _ => false
    }
  }

  def get(index: Int, flow: Option[String] = None) =
    Authorised.async {
      implicit authContext => implicit request =>
        fetchModel flatMap {
          case Some(data) => {
            data.lift(index - 1) match {
              case Some(x) => showHideAddressMove(x.lineId) map {showHide =>
                Ok(views.html.responsiblepeople.detailed_answers(Some(x), index, showHide, ControllerHelper.rpTitleName(Some(x)), flow))
              }
              case _ => Future.successful(NotFound(notFoundView))
            }
          }
          case _ => Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
        }
    }

  def post(index: Int, flow: Option[String] = None) = Authorised.async{
    implicit authContext => implicit request =>
      updateDataStrict[ResponsiblePerson](index){ rp =>
        rp.copy(hasAccepted = true)
      } flatMap { _ =>
        flow match {
          case Some(`flowFromDeclaration`) => redirectFromDeclarationFlow()
          case Some(`flowChangeOfficer`) => Future.successful(Redirect(controllers.changeofficer.routes.NewOfficerController.get()))
          case None => Future.successful(Redirect(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get()))
        }
      }
  }

  private def redirectFromDeclarationFlow()(implicit hc: HeaderCarrier, authContext: AuthContext) =
    (for {
      model <- OptionT(fetchModel)
      _ <- OptionT.liftF(dataCacheConnector.save(ResponsiblePerson.key, model.filterEmpty.map(_.copy(hasAccepted = true))))
      hasNominatedOfficer <- OptionT.liftF(ControllerHelper.hasNominatedOfficer(dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key)))
      businessmatching <- OptionT.liftF(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
      reviewDetails <- OptionT.fromOption[Future](businessmatching.reviewDetails)
      businessType <- OptionT.fromOption[Future](reviewDetails.businessType)
      status <- OptionT.liftF(statusService.getStatus)
    } yield businessType match {
      case Partnership if DeclarationHelper.numberOfPartners(model) < 2 =>
        Redirect(controllers.declaration.routes.RegisterPartnersController.get())
      case _ =>
        Redirect(DeclarationHelper.routeDependingOnNominatedOfficer(hasNominatedOfficer, status, config.showFeesToggle))
    }) getOrElse InternalServerError("Cannot determine redirect")

  private def fetchModel(implicit authContext: AuthContext, hc: HeaderCarrier) =
    dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key)

}

object DetailedAnswersController extends DetailedAnswersController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override protected def statusService: StatusService = StatusService
  override protected  def config = Play.current.injector.instanceOf[AppConfig]
}
