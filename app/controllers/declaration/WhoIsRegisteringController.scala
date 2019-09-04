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

package controllers.declaration

import com.google.inject.Inject
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.declaration._
import models.declaration.release7.RoleWithinBusinessRelease7
import models.responsiblepeople.{PositionWithinBusiness, ResponsiblePerson}
import models.status._
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import views.html.declaration.{who_is_registering_this_registration, who_is_registering_this_renewal, who_is_registering_this_update}

import scala.concurrent.Future

class WhoIsRegisteringController @Inject () (
                                            authAction: AuthAction, val ds: CommonPlayDependencies,
                                            val dataCacheConnector: DataCacheConnector,
                                            val statusService: StatusService,
                                            val renewalService: RenewalService,
                                            val amlsConnector: AmlsConnector
                                            ) extends AmlsBaseController(ds) {

  def get = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
          } yield whoIsRegisteringView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, EmptyForm, ResponsiblePerson.filter(responsiblePeople))
          ) getOrElse whoIsRegisteringView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, EmptyForm, Seq.empty)
      }
  }

  def post: Action[AnyContent] = authAction.async {
    implicit request => {
      Form2[WhoIsRegistering](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key) flatMap {
            case Some(data) =>
              whoIsRegisteringViewWithError(request.amlsRefNumber, request.accountTypeId, request.credId, BadRequest, f, ResponsiblePerson.filter(data))
            case None => whoIsRegisteringViewWithError(request.amlsRefNumber, request.accountTypeId, request.credId, BadRequest, f, Seq.empty)
          }
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll(request.credId) flatMap {
            optionalCache =>
              (for {
                cache <- optionalCache
                responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
              } yield {
                data.person match {
                  case "-1" =>
                    redirectToAddPersonPage(request.amlsRefNumber, request.accountTypeId, request.credId)
                  case _ =>
                    getAddPerson(data, ResponsiblePerson.filter(responsiblePeople)) map { addPerson =>
                      dataCacheConnector.save[AddPerson](request.credId, AddPerson.key, addPerson) flatMap {
                        _ => redirectToDeclarationPage(request.amlsRefNumber, request.accountTypeId, request.credId)
                      }
                    } getOrElse Future.successful(NotFound(notFoundView))
                }
              }) getOrElse redirectToDeclarationPage(request.amlsRefNumber, request.accountTypeId, request.credId)
          }
      }
    }
  }

  def whoIsRegisteringViewWithError(amlsRegistrationNo: Option[String],
                                    accountTypeId: (String, String),
                                    cacheId: String,
                                    status: Status,
                                    form: InvalidForm,
                                    rp: Seq[ResponsiblePerson])
                                   (implicit request: Request[AnyContent]): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) flatMap {
      case a@(SubmissionReadyForReview | SubmissionDecisionApproved | ReadyForRenewal(_)) =>
        renewalService.getRenewal(cacheId) map {
          case Some(_) =>
            val updatedForm = updateFormErrors(form, a, renewal = true)
            status(who_is_registering_this_renewal(updatedForm, rp))
          case _ =>
            val updatedForm = updateFormErrors(form, a, renewal = false)
            status(who_is_registering_this_update(updatedForm, rp))
        }
      case b@RenewalSubmitted(_) =>
        val updatedForm = updateFormErrors(form, b, renewal = true)
        Future.successful(status(who_is_registering_this_update(updatedForm, rp)))
      case _ =>
        Future.successful(status(who_is_registering_this_registration(form, rp)))
    }

  def updateFormErrors(f: InvalidForm, status: SubmissionStatus, renewal: Boolean): InvalidForm = {
    val common = "error.required.declaration.who.is.declaring.this"
    status match {
      case SubmissionReadyForReview | SubmissionDecisionApproved | ReadyForRenewal(_) =>
        if (renewal) {
          f.copy(errors = Seq((Path("person"), Seq(ValidationError(Seq(Messages(s"$common.renewal")))))))
        } else {
          f.copy(errors = Seq((Path("person"), Seq(ValidationError(Seq(Messages(s"$common.update")))))))
        }
      case RenewalSubmitted(_) =>
        f.copy(errors = Seq((Path("person"), Seq(ValidationError(Seq(Messages(s"$common.update")))))))
      case _ =>
        f.copy(errors = Seq((Path("person"), Seq(ValidationError(Seq(Messages("error.required.declaration.who.is.registering")))))))
    }
  }

  private def whoIsRegisteringView (amlsRegistrationNo: Option[String],
                                    accountTypeId: (String, String),
                                    cacheId: String,
                                    status: Status,
                                    form: Form2[WhoIsRegistering],
                                    rp: Seq[ResponsiblePerson])
                                  (implicit request: Request[AnyContent]): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) flatMap {
      case SubmissionReadyForReview | SubmissionDecisionApproved | ReadyForRenewal(_) =>
        renewalService.getRenewal(cacheId) map {
          case Some(_) =>
            status(who_is_registering_this_renewal(form, rp))
          case _ =>
            status(who_is_registering_this_update(form, rp))
        }
      case RenewalSubmitted(_) =>
        Future.successful(status(who_is_registering_this_update(form, rp)))
      case _ =>
        Future.successful(status(who_is_registering_this_registration(form, rp)))
    }

  private def redirectToDeclarationPage(amlsRegistrationNo: Option[String],
                                        accountTypeId: (String, String),
                                        cacheId: String)(implicit hc: HeaderCarrier): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReadyForReview | SubmissionDecisionApproved => Redirect(routes.DeclarationController.getWithAmendment())
      case _ => Redirect(routes.DeclarationController.get())
    }

  private def redirectToAddPersonPage(amlsRegistrationNo: Option[String],
                                      accountTypeId: (String, String),
                                      cacheId: String)(implicit hc: HeaderCarrier): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReadyForReview | SubmissionDecisionApproved => Redirect(routes.AddPersonController.getWithAmendment())
      case _ => Redirect(routes.AddPersonController.get())
    }

  private def getAddPerson(whoIsRegistering: WhoIsRegistering, responsiblePeople: Seq[ResponsiblePerson]): Option[AddPerson] = {
    for {
      selectedIndex <- whoIsRegistering.indexValue
      selectedPerson <- responsiblePeople.zipWithIndex.collect {
        case (person, i) if i == selectedIndex => person
      }.headOption
      personName <- selectedPerson.personName
    } yield {
      AddPerson(
        personName.firstName,
        personName.middleName,
        personName.lastName,
        selectedPerson.positions.fold[Set[PositionWithinBusiness]](Set.empty)(x => x.positions)
      )
    }
  }

  private implicit def getPosition(positions: Set[PositionWithinBusiness]): RoleWithinBusinessRelease7 = {
    import models.responsiblepeople._

    RoleWithinBusinessRelease7(
      positions.map {
        case BeneficialOwner => models.declaration.release7.BeneficialShareholder
        case Director => models.declaration.release7.Director
        case InternalAccountant => models.declaration.release7.InternalAccountant
        case NominatedOfficer => models.declaration.release7.NominatedOfficer
        case Partner => models.declaration.release7.Partner
        case SoleProprietor => models.declaration.release7.SoleProprietor
        case DesignatedMember => models.declaration.release7.DesignatedMember
        case Other(d) => models.declaration.release7.Other(d)
      }
    )
  }
}
