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

package controllers.declaration

import config.AMLSAuthConnector
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration._
import models.declaration.release7.RoleWithinBusinessRelease7
import models.renewal.Renewal
import models.responsiblepeople.{PositionWithinBusiness, ResponsiblePeople}
import models.status._
import play.api.Play
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{RenewalService, StatusService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.StatusConstants
import views.html.declaration.{who_is_registering_this_registration, who_is_registering_this_renewal, who_is_registering_this_update}

import scala.concurrent.Future

trait WhoIsRegisteringController extends BaseController {

  private[controllers] def amlsConnector: AmlsConnector
  private[controllers] def renewalService: RenewalService

  def dataCacheConnector: DataCacheConnector
  def statusService: StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          } yield whoIsRegisteringView(Ok, EmptyForm, responsiblePeople.filter(!_.status.contains(StatusConstants.Deleted)))
          ) getOrElse whoIsRegisteringView(Ok, EmptyForm, Seq.empty)
      }
  }

  def getAddPerson(whoIsRegistering: WhoIsRegistering, responsiblePeople: Seq[ResponsiblePeople]): Option[AddPerson] = {

    val rpOption = responsiblePeople.find(_.personName.exists(name => whoIsRegistering.person.equals(name.firstName.concat(name.lastName))))
    val rp: ResponsiblePeople = rpOption.getOrElse(ResponsiblePeople.default(None))

    rp.personName match {
      case Some(name) => Some(AddPerson(name.firstName, name.middleName, name.lastName,
        rp.positions.fold[Set[PositionWithinBusiness]](Set.empty)(x => x.positions)))
      case _ => None
    }
  }

  implicit def getPosition(positions: Set[PositionWithinBusiness]): RoleWithinBusinessRelease7 = {
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
    }
    )
  }

  def post: Action[AnyContent] = Authorised.async {
    implicit authContext => implicit request => {
      Form2[WhoIsRegistering](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) flatMap {
            case Some(data) => whoIsRegisteringView(BadRequest, f, data.filter(!_.status.contains(StatusConstants.Deleted)))
            case None => whoIsRegisteringView(BadRequest, f, Seq.empty)
          }
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll flatMap {
            optionalCache =>
              (for {
                cache <- optionalCache
                responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
              } yield {
                dataCacheConnector.save[WhoIsRegistering](WhoIsRegistering.key, data)
                data.person match {
                  case "-1" => {
                    redirectToAddPersonPage
                  }
                  case _ => {
                    getAddPerson(data, responsiblePeople.filter(!_.status.contains(StatusConstants.Deleted))) map { addPerson =>
                      dataCacheConnector.save[AddPerson](AddPerson.key, addPerson)
                    }
                    redirectToDeclarationPage
                  }
                }
              }) getOrElse redirectToDeclarationPage
          }
      }
    }
  }

  private def whoIsRegisteringView(status: Status, form: Form2[WhoIsRegistering], rp: Seq[ResponsiblePeople])
                                  (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] =
    statusService.getStatus flatMap {
      case SubmissionReadyForReview | SubmissionDecisionApproved | ReadyForRenewal(_) if AmendmentsToggle.feature =>
        renewalService.getRenewal map {
          case Some(_) => status(who_is_registering_this_renewal(form, rp))
          case _ => status(who_is_registering_this_update(form, rp))
        }
      case RenewalSubmitted(_) => Future.successful(status(who_is_registering_this_update(form, rp)))
      case _ => Future.successful(status(who_is_registering_this_registration(form, rp)))
    }

  private def redirectToDeclarationPage(implicit hc: HeaderCarrier, auth: AuthContext): Future[Result] =
    statusService.getStatus map {
      case SubmissionReadyForReview | SubmissionDecisionApproved if AmendmentsToggle.feature => Redirect(routes.DeclarationController.getWithAmendment())
      case _ => Redirect(routes.DeclarationController.get())
    }

  private def redirectToAddPersonPage(implicit hc: HeaderCarrier, auth: AuthContext): Future[Result] =
    statusService.getStatus map {
      case SubmissionReadyForReview | SubmissionDecisionApproved if AmendmentsToggle.feature => Redirect(routes.AddPersonController.getWithAmendment())
      case _ => Redirect(routes.AddPersonController.get())
    }

}

object WhoIsRegisteringController extends WhoIsRegisteringController {
  // $COVERAGE-OFF$
  override private[controllers] val amlsConnector: AmlsConnector = AmlsConnector
  override private[controllers] val renewalService = Play.current.injector.instanceOf[RenewalService]
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
