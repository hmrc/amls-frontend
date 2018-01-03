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

package controllers.declaration

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration.BusinessPartners
import models.responsiblepeople.ResponsiblePeople._
import models.responsiblepeople.{Partner, Positions, ResponsiblePeople}
import models.status.{RenewalSubmitted, _}
import play.api.mvc.{AnyContent, Request, Result}
import services.{ProgressService, StatusService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.DeclarationHelper._
import utils.StatusConstants
import views.html.declaration.register_partners

import scala.concurrent.Future

@Singleton
class RegisterPartnersController @Inject()(val authConnector: AuthConnector,
                                           val dataCacheConnector: DataCacheConnector,
                                           implicit val statusService: StatusService,
                                           implicit val progressService: ProgressService
                                          ) extends BaseController {

  def businessPartnersView(status: Status, form: Form2[BusinessPartners], rp: Seq[ResponsiblePeople])
                          (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] = {
    statusService.getStatus map {
      case SubmissionReady =>
        status(register_partners("submit.registration", form, getNonPartners(rp), currentPartnersNames(rp)))
      case SubmissionReadyForReview | SubmissionDecisionApproved =>
        status(register_partners("submit.amendment.application", form, getNonPartners(rp), currentPartnersNames(rp)))
      case ReadyForRenewal(_) | RenewalSubmitted(_) =>
        status(register_partners("submit.renewal.application", form, getNonPartners(rp), currentPartnersNames(rp)))
      case _ =>
        throw new Exception("Incorrect status - Page not permitted for this status")
    }
  }

  private def saveAndRedirect(data : BusinessPartners) (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] = {
    (for {
      responsiblePeople <- dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
      rp <- updatePartners(responsiblePeople, data)
      _ <- dataCacheConnector.save(ResponsiblePeople.key, rp)
      url <- progressService.getSubmitRedirect
    } yield url match {
      case Some(x) => Redirect(x)
      case _ => InternalServerError("Unable to get redirect url")
    }) recoverWith {
      case _ : Throwable => Future.successful(InternalServerError("Unable to save data and get redirect link"))
    }
  }

  def updatePartners(eventualMaybePeoples: Option[Seq[ResponsiblePeople]],
                     data: BusinessPartners): Future[Option[Seq[ResponsiblePeople]]] = {
    eventualMaybePeoples match {
      case Some(rpSeq) =>
        val updatedList = ResponsiblePeople.filter(rpSeq).map { responsiblePerson =>
          responsiblePerson.personName.exists(name => name.fullNameWithoutSpace.equals(data.value)) match {
            case true =>
              val position = responsiblePerson.positions.fold[Option[Positions]](None)(p => Some(Positions(p.positions + Partner, p.startDate)))
              responsiblePerson.copy(positions = position)
            case false => responsiblePerson
          }
        }
        Future.successful(Some(updatedList))
      case _ => Future.successful(eventualMaybePeoples)
    }
  }

  def getNonPartners(people: Seq[ResponsiblePeople]) = {
    people.filter(_.positions.fold(false)(p => !p.positions.contains(Partner)))
  }

  def get() = Authorised.async {
    implicit authContext => implicit request => {

      val result = for {
        subtitle <- OptionT.liftF(statusSubtitle())
        responsiblePeople <- OptionT(dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
      } yield {
        Ok(views.html.declaration.register_partners(
          subtitle,
          EmptyForm,
          nonPartners(responsiblePeople),
          currentPartnersNames(responsiblePeople)
        ))
      }
      result getOrElse InternalServerError("failure getting status")
    }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BusinessPartners](request.body) match {
        case f: InvalidForm => {
          dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) flatMap {
            case Some(data) => {
              businessPartnersView(BadRequest, f, data)
            }
            case None =>
              businessPartnersView(BadRequest, f, Seq.empty)
          }
        }
        case ValidForm(_, data) => {
          data.value match {
            case "-1" =>
              Future.successful(Redirect(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true, Some(flowFromDeclaration))))
            case _ =>
              saveAndRedirect(data)
          }
        }
      }
    }
  }
}
