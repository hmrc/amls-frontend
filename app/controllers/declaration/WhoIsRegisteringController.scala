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
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.declaration.WhoIsRegisteringFormProvider
import models.declaration._
import models.declaration.release7.RoleWithinBusinessRelease7
import models.responsiblepeople.{PositionWithinBusiness, ResponsiblePerson}
import models.status._
import play.api.data.Form
import play.api.mvc._
import services.{RenewalService, SectionsProvider, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, DeclarationHelper}
import views.html.declaration.{WhoIsRegisteringThisRegistrationView, WhoIsRegisteringThisRenewalView, WhoIsRegisteringThisUpdateView}

import scala.concurrent.Future

class WhoIsRegisteringController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val renewalService: RenewalService,
  val amlsConnector: AmlsConnector,
  val cc: MessagesControllerComponents,
  val sectionsProvider: SectionsProvider,
  formProvider: WhoIsRegisteringFormProvider,
  renewalView: WhoIsRegisteringThisRenewalView,
  updateView: WhoIsRegisteringThisUpdateView,
  registrationView: WhoIsRegisteringThisRegistrationView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    lazy val renderProperViewWhenSectionsComplete = dataCacheConnector.fetchAll(request.credId) flatMap {
      optionalCache =>
        (for {
          cache             <- optionalCache
          responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
        } yield whoIsRegisteringView(
          request.amlsRefNumber,
          request.accountTypeId,
          request.credId,
          Ok,
          ResponsiblePerson.filter(responsiblePeople)
        )) getOrElse whoIsRegisteringView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, Seq.empty)
    }

    for {
      isRenewal        <- renewalService.isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId)
      sectionsComplete <- DeclarationHelper.sectionsComplete(request.credId, sectionsProvider, isRenewal)
      result           <-
        if (sectionsComplete)
          renderProperViewWhenSectionsComplete
        else
          Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
    } yield result
  }

  def post(identifier: String): Action[AnyContent] = authAction.async { implicit request =>
    getFormProvider(identifier)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key) flatMap {
            case Some(data) =>
              whoIsRegisteringViewWithError(
                request.amlsRefNumber,
                request.accountTypeId,
                request.credId,
                BadRequest,
                formWithErrors,
                ResponsiblePerson.filter(data)
              )
            case None       =>
              whoIsRegisteringViewWithError(
                request.amlsRefNumber,
                request.accountTypeId,
                request.credId,
                BadRequest,
                formWithErrors,
                Seq.empty
              )
          },
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap { optionalCache =>
            (for {
              cache             <- optionalCache
              responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
            } yield data.person match {
              case "-1" =>
                redirectToAddPersonPage(request.amlsRefNumber, request.accountTypeId, request.credId)
              case _    =>
                getAddPerson(data, ResponsiblePerson.filter(responsiblePeople)) map { addPerson =>
                  dataCacheConnector.save[AddPerson](request.credId, AddPerson.key, addPerson) flatMap { _ =>
                    redirectToDeclarationPage(request.amlsRefNumber, request.accountTypeId, request.credId)
                  }
                } getOrElse Future.successful(NotFound(notFoundView))
            }) getOrElse redirectToDeclarationPage(request.amlsRefNumber, request.accountTypeId, request.credId)
          }
      )
  }

  def whoIsRegisteringViewWithError(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String,
    status: Status,
    formWithErrors: Form[WhoIsRegistering],
    rp: Seq[ResponsiblePerson]
  )(implicit request: Request[AnyContent]): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) flatMap {
      case (SubmissionReadyForReview | SubmissionDecisionApproved | ReadyForRenewal(_)) =>
        renewalService.getRenewal(cacheId) map {
          case Some(_) =>
            status(renewalView(formWithErrors, rp))
          case _       =>
            status(updateView(formWithErrors, rp))
        }
      case RenewalSubmitted(_)                                                          =>
        Future.successful(status(updateView(formWithErrors, rp)))
      case _                                                                            =>
        Future.successful(status(registrationView(formWithErrors, rp)))
    }

  private def whoIsRegisteringView(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String,
    status: Status,
    rp: Seq[ResponsiblePerson]
  )(implicit request: Request[AnyContent]): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) flatMap {
      case SubmissionReadyForReview | SubmissionDecisionApproved | ReadyForRenewal(_) =>
        renewalService.getRenewal(cacheId) map {
          case Some(_) =>
            status(renewalView(getFormProvider("renewal"), rp))
          case _       =>
            status(updateView(getFormProvider("update"), rp))
        }
      case RenewalSubmitted(_)                                                        =>
        Future.successful(status(updateView(getFormProvider("update"), rp)))
      case _                                                                          =>
        Future.successful(status(registrationView(getFormProvider("registration"), rp)))
    }

  private def getFormProvider(str: String): Form[WhoIsRegistering] = str match {
    case r @ "renewal" => formProvider(r)
    case u @ "update"  => formProvider(u)
    case _             => formProvider("registration")
  }

  private def redirectToDeclarationPage(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String
  )(implicit hc: HeaderCarrier): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReadyForReview | SubmissionDecisionApproved =>
        Redirect(routes.DeclarationController.getWithAmendment())
      case _                                                     => Redirect(routes.DeclarationController.get())
    }

  private def redirectToAddPersonPage(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String
  )(implicit hc: HeaderCarrier): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReadyForReview | SubmissionDecisionApproved =>
        Redirect(routes.AddPersonController.getWithAmendment())
      case _                                                     => Redirect(routes.AddPersonController.get())
    }

  private def getAddPerson(
    whoIsRegistering: WhoIsRegistering,
    responsiblePeople: Seq[ResponsiblePerson]
  ): Option[AddPerson] =
    for {
      selectedIndex  <- whoIsRegistering.indexValue
      selectedPerson <- responsiblePeople.zipWithIndex.collectFirst {
                          case (person, i) if i == selectedIndex => person
                        }
      personName     <- selectedPerson.personName
    } yield AddPerson(
      personName.firstName,
      personName.middleName,
      personName.lastName,
      selectedPerson.positions.fold[Set[PositionWithinBusiness]](Set.empty)(x => x.positions)
    )

  private implicit def getPosition(positions: Set[PositionWithinBusiness]): RoleWithinBusinessRelease7 = {
    import models.responsiblepeople._

    RoleWithinBusinessRelease7(
      positions.map {
        case BeneficialOwner    => models.declaration.release7.BeneficialShareholder
        case Director           => models.declaration.release7.Director
        case InternalAccountant => models.declaration.release7.InternalAccountant
        case NominatedOfficer   => models.declaration.release7.NominatedOfficer
        case Partner            => models.declaration.release7.Partner
        case SoleProprietor     => models.declaration.release7.SoleProprietor
        case DesignatedMember   => models.declaration.release7.DesignatedMember
        case Other(d)           => models.declaration.release7.Other(d)
      }
    )
  }
}
