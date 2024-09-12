/*
 * Copyright 2024 HM Revenue & Customs
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
import com.google.inject.Inject
import config.{AmlsErrorHandler, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.Partnership
import models.responsiblepeople.ResponsiblePerson
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.StatusService
import services.businessmatching.RecoverActivitiesService
import uk.gov.hmrc.http.HeaderCarrier
import services.cache.Cache
import utils.responsiblepeople.CheckYourAnswersHelper
import utils.{AuthAction, ControllerHelper, DeclarationHelper, RepeatingSection}
import views.html.responsiblepeople.CheckYourAnswersView

import scala.concurrent.Future

class DetailedAnswersController @Inject () (
                                             val dataCacheConnector: DataCacheConnector,
                                             val recoverActivitiesService: RecoverActivitiesService,
                                             authAction: AuthAction,
                                             val ds: CommonPlayDependencies,
                                             val statusService: StatusService,
                                             val config: ApplicationConfig,
                                             val cc: MessagesControllerComponents,
                                             cyaHelper: CheckYourAnswersHelper,
                                             view: CheckYourAnswersView,
                                             amlsErrorHandler: AmlsErrorHandler,
                                             implicit val error: views.html.ErrorView)
  extends AmlsBaseController(ds, cc) with RepeatingSection with Logging {

  private def showHideAddressMove(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String, lineId: Option[Int])
                                 (implicit headerCarrier: HeaderCarrier): Future[Boolean] = {
    statusService.getStatus(amlsRegistrationNo, accountTypeId, credId) map {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) if lineId.isDefined => true
      case _ => false
    }
  }

  def get(index: Int, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) flatMap {
        optionalCache =>
          (for {
            cache: Cache <- optionalCache
            businessMatching: BusinessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          } yield {
            redirect(request.amlsRefNumber, request.accountTypeId, request.credId, cache, index, flow, businessMatching)
          }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
      } recoverWith {
        case _: NoSuchElementException =>
          logger.warn("[DetailedAnswersController][get] - Business activities list was empty, attempting to recover")
          recoverActivitiesService.recover(request).flatMap {
            case true => Future.successful(Redirect(routes.DetailedAnswersController.get(index, flow)))
            case false =>
              logger.warn("[DetailedAnswersController][get] - Unable to determine business types")
              amlsErrorHandler.internalServerErrorTemplate.map { errorPage =>
                InternalServerError(errorPage)
              }
          }
      }
  }


  private def redirect(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String, cache: Cache, index: Int, flow: Option[String], businessMatching: BusinessMatching)
                      (implicit request: Request[_]): Future[Result] =
    (for {
      responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
    } yield responsiblePeople.lift(index - 1) match {
      case Some(x) if x.copy(hasAccepted = true).isComplete => showHideAddressMove(amlsRegistrationNo, accountTypeId, credId, x.lineId) flatMap { showHide =>
        isMsbOrTcsp(credId).map {
          msbOrTcsp: Option[Boolean] =>
            val shouldShowApprovalSection = !msbOrTcsp.contains(true) && x.approvalFlags.hasAlreadyPassedFitAndProper.contains(false)
            val personName = ControllerHelper.rpTitleName(Some(x))
            val summaryList = cyaHelper.getHeadingsAndSummaryLists(x, businessMatching, personName, index, flow, showHide, shouldShowApprovalSection)
            Ok(view(summaryList, index, showHide, personName, flow))
        }
      }
      case Some(_) => Future.successful(Redirect(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get()))
      case _ => Future.successful(NotFound(notFoundView))
    }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))

  def post(index: Int, flow: Option[String] = None): Action[AnyContent] = authAction.async{
    implicit request =>
      updateDataStrict[ResponsiblePerson](request.credId, index){ rp =>
        rp.copy(hasAccepted = true)
      } flatMap { _ =>
        flow match {
          case Some(`flowFromDeclaration`) => redirectFromDeclarationFlow(request.amlsRefNumber, request.accountTypeId, request.credId)
          case _ => Future.successful(Redirect(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get()))
        }
      }
  }

  private def isMsbOrTcsp(credId: String): Future[Option[Boolean]] =
    for {
      businessmatching <- dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key)
    } yield businessmatching.map(_.msbOrTcsp)

  private def redirectFromDeclarationFlow(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)(implicit hc: HeaderCarrier): Future[Result] =
    (for {
      model <- OptionT(fetchModel(credId))
      _ <- OptionT.liftF(dataCacheConnector.save(credId, ResponsiblePerson.key, model.filterEmpty.map(_.copy(hasAccepted = true))))
      hasNominatedOfficer <- OptionT.liftF(ControllerHelper.hasNominatedOfficer(dataCacheConnector.fetch[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key)))
      businessmatching <- OptionT.liftF(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
      reviewDetails <- OptionT.fromOption[Future](businessmatching.reviewDetails)
      businessType <- OptionT.fromOption[Future](reviewDetails.businessType)
      status <- OptionT.liftF(statusService.getStatus(amlsRegistrationNo, accountTypeId, credId))
    } yield businessType match {
      case Partnership if DeclarationHelper.numberOfPartners(model) < 2 =>
        Redirect(controllers.declaration.routes.RegisterPartnersController.get())
      case _ =>
        Redirect(DeclarationHelper.routeDependingOnNominatedOfficer(hasNominatedOfficer, status))
    }) getOrElse InternalServerError("Cannot determine redirect")

  private def fetchModel(credId: String): Future[Option[Seq[ResponsiblePerson]]] =
    dataCacheConnector.fetch[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key)

}