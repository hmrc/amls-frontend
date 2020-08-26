/*
 * Copyright 2020 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.Partnership
import models.responsiblepeople.ResponsiblePerson
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved}
import play.api.mvc.{MessagesControllerComponents, Request}
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, ControllerHelper, DeclarationHelper, RepeatingSection}
import views.html.responsiblepeople.detailed_answers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DetailedAnswersController @Inject () (
                                             val dataCacheConnector: DataCacheConnector,
                                             authAction: AuthAction,
                                             val ds: CommonPlayDependencies,
                                             val statusService: StatusService,
                                             val config: ApplicationConfig,
                                             val cc: MessagesControllerComponents,
                                             detailed_answers: detailed_answers,
                                             implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  private def showHideAddressMove(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String, lineId: Option[Int])
                                 (implicit headerCarrier: HeaderCarrier): Future[Boolean] = {
    statusService.getStatus(amlsRegistrationNo, accountTypeId, credId) map {
      case SubmissionDecisionApproved | ReadyForRenewal(_) | RenewalSubmitted(_) if lineId.isDefined => true
      case _ => false
    }
  }

  def get(index: Int, flow: Option[String] = None) = authAction.async {
      implicit request =>
        dataCacheConnector.fetchAll(request.credId) flatMap {
          optionalCache =>
            (for {
              cache: CacheMap <- optionalCache
              businessMatching: BusinessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {
              redirect(request.amlsRefNumber, request.accountTypeId, request.credId, cache, index, flow, businessMatching)
            }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
        }
  }

  private def redirect(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String, cache: CacheMap, index: Int, flow: Option[String] = None, businessMatching: BusinessMatching)
                      (implicit request: Request[_]) =
    (for {
      responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
    } yield responsiblePeople.lift(index - 1) match {
      case Some(x) if x.copy(hasAccepted = true).isComplete => showHideAddressMove(amlsRegistrationNo, accountTypeId, credId, x.lineId) flatMap { showHide =>
        isMsbOrTcsp(credId).map {
          msbOrTcsp: Option[Boolean] =>

            val shouldShowApprovalSection = !(msbOrTcsp.contains(true)) && x.approvalFlags.hasAlreadyPassedFitAndProper.contains(false)
            Ok(detailed_answers(Some(x), index, showHide, ControllerHelper.rpTitleName(Some(x)), flow, shouldShowApprovalSection, businessMatching))
        }
      }
      case Some(_) => Future.successful(Redirect(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get()))
      case _ => Future.successful(NotFound(notFoundView))
    }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))

  def post(index: Int, flow: Option[String] = None) = authAction.async{
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

  private def isMsbOrTcsp(credId: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    for {
      businessmatching <- dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key)
    } yield businessmatching.map(_.msbOrTcsp)
  }

  private def redirectFromDeclarationFlow(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)(implicit hc: HeaderCarrier) =
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

  private def fetchModel(credId: String)(implicit hc: HeaderCarrier) =
    dataCacheConnector.fetch[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key)

}