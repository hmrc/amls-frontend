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

package controllers.declaration

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.declaration.AddPerson
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved, SubmissionReadyForReview}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{RenewalService, SectionsProvider, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, DeclarationHelper}

import scala.concurrent.Future
import views.html.declaration.DeclareView

class DeclarationController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  val sectionsProvider: SectionsProvider,
  val renewalService: RenewalService,
  view: DeclareView
) extends AmlsBaseController(ds, cc)
    with Logging {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    // TODO: update state should happen on Post method, not on the Get, which should be readonly and idempotent
    lazy val whenSectionsComplete: Future[Result] =
      dataCacheConnector.fetch[AddPerson](request.credId, AddPerson.key) flatMap {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName getOrElse ""} ${addPerson.lastName}"
          for {
            renewalComplete <- DeclarationHelper.renewalComplete(renewalService, request.credId)
            status          <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
          } yield status match {
            case ReadyForRenewal(_) if renewalComplete                                                            =>
              Ok(
                view("declaration.declaration.amendment.title", "submit.renewal.application", name, isAmendment = false)
              )
            case ReadyForRenewal(_) | SubmissionReadyForReview | SubmissionDecisionApproved | RenewalSubmitted(_) =>
              Ok(
                view(
                  "declaration.declaration.amendment.title",
                  "submit.amendment.application",
                  name,
                  isAmendment = true
                )
              )
            case _                                                                                                =>
              Ok(view("declaration.declaration.amendment.title", "submit.registration", name, isAmendment = false))
          }
        case _               => redirectToAddPersonPage(request.amlsRefNumber, request.accountTypeId, request.credId)
      }

    for {
      isRenewal        <- renewalService.isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId)
      sectionsComplete <- DeclarationHelper.sectionsComplete(request.credId, sectionsProvider, isRenewal)
      result           <- sectionsComplete match {
                            case true  => whenSectionsComplete
                            case false =>
                              logger.info("sections not complete, redirecting")
                              Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
                          }
    } yield result
  }

  def getWithAmendment: Action[AnyContent] =
    declarationView("declaration.declaration.amendment.title", "submit.amendment.application", true)

  private def declarationView(title: String, subtitle: String, isAmendment: Boolean): Action[AnyContent] =
    authAction.async { implicit request =>
      // TODO: update state should happen on Post method, not on the Get, which should be readonly and idempotent
      lazy val whenSectionsComplete = dataCacheConnector.fetch[AddPerson](request.credId, AddPerson.key) flatMap {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName getOrElse ""} ${addPerson.lastName}"
          Future.successful(Ok(view(title, subtitle, name, isAmendment)))
        case _               =>
          logger.warn("No 'addPerson' found, redirecting to redirectToAddPersonPage")
          redirectToAddPersonPage(request.amlsRefNumber, request.accountTypeId, request.credId)
      }
      for {
        isRenewal        <- renewalService.isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId)
        sectionsComplete <- DeclarationHelper.sectionsComplete(request.credId, sectionsProvider, isRenewal)
        result           <- sectionsComplete match {
                              case true  => whenSectionsComplete
                              case false =>
                                Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
                            }
      } yield result
    }

  private def redirectToAddPersonPage(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String
  )(implicit hc: HeaderCarrier): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReadyForReview => Redirect(routes.AddPersonController.getWithAmendment())
      case _                        => Redirect(routes.AddPersonController.get())
    }

}
