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

import javax.inject.{Inject, Singleton}
import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.declaration.BusinessPartnersFormProvider
import models.declaration.BusinessPartners
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{Partner, Positions, ResponsiblePerson}
import models.status.{RenewalSubmitted, _}
import play.api.Logging
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{ProgressService, RenewalService, SectionsProvider, StatusService}
import utils.DeclarationHelper._
import utils.{AuthAction, DeclarationHelper}
import views.html.declaration.RegisterPartnersView

import scala.concurrent.Future

@Singleton
class RegisterPartnersController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val progressService: ProgressService,
  val cc: MessagesControllerComponents,
  val sectionsProvider: SectionsProvider,
  formProvider: BusinessPartnersFormProvider,
  renewalService: RenewalService,
  view: RegisterPartnersView
) extends AmlsBaseController(ds, cc)
    with Logging {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    lazy val whenSectionsComplete = {
      val result = for {
        subtitle          <- OptionT.liftF(statusSubtitle(request.amlsRefNumber, request.accountTypeId, request.credId))
        responsiblePeople <-
          OptionT(dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key))
      } yield Ok(
        view(
          subtitle,
          formProvider(),
          nonPartners(responsiblePeople),
          currentPartnersNames(responsiblePeople)
        )
      )
      // TODO: throw exception so it's possible to see why it failed
      result getOrElse InternalServerError("failure getting status")
    }

    for {
      isRenewal        <- renewalService.isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId)
      sectionsComplete <- DeclarationHelper.sectionsComplete(request.credId, sectionsProvider, isRenewal)
      result           <- sectionsComplete match {
                            case true  => whenSectionsComplete
                            case false =>
                              logger.warn("Sections aren't complete, redirecting")
                              Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
                          }
    } yield result
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key) flatMap {
            case Some(data) =>
              businessPartnersView(
                request.amlsRefNumber,
                request.accountTypeId,
                request.credId,
                BadRequest,
                formWithErrors,
                data
              )
            case None       =>
              businessPartnersView(
                request.amlsRefNumber,
                request.accountTypeId,
                request.credId,
                BadRequest,
                formWithErrors,
                Seq.empty
              )
          },
        data =>
          data.value match {
            case "-1" =>
              Future.successful(
                Redirect(
                  controllers.responsiblepeople.routes.ResponsiblePeopleAddController
                    .get(true, Some(flowFromDeclaration))
                )
              )
            case _    =>
              saveAndRedirect(request.amlsRefNumber, request.accountTypeId, request.credId, data)
          }
      )
  }

  private def getNonPartners(people: Seq[ResponsiblePerson]) =
    people.filter(_.positions.fold(false)(p => !p.positions.contains(Partner)))

  private def businessPartnersView(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String,
    status: Status,
    form: Form[BusinessPartners],
    rp: Seq[ResponsiblePerson]
  )(implicit request: Request[AnyContent]): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReady                                       =>
        status(view("submit.registration", form, getNonPartners(rp), currentPartnersNames(rp)))
      case SubmissionReadyForReview | SubmissionDecisionApproved =>
        status(view("submit.amendment.application", form, getNonPartners(rp), currentPartnersNames(rp)))
      case ReadyForRenewal(_) | RenewalSubmitted(_)              =>
        status(view("submit.renewal.application", form, getNonPartners(rp), currentPartnersNames(rp)))
      case _                                                     =>
        throw new Exception("Incorrect status - Page not permitted for this status")
    }

  private def saveAndRedirect(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String,
    data: BusinessPartners
  )(implicit request: Request[AnyContent]): Future[Result] =
    (for {
      responsiblePeople <- dataCacheConnector.fetch[Seq[ResponsiblePerson]](cacheId, ResponsiblePerson.key)
      rp                <- updatePartners(responsiblePeople, data)
      _                 <- dataCacheConnector.save(cacheId, ResponsiblePerson.key, rp)
      url               <- progressService.getSubmitRedirect(amlsRegistrationNo, accountTypeId, cacheId)
    } yield url match {
      case Some(x) => Redirect(x)
      case _       => InternalServerError("Unable to get redirect url")
    }) recoverWith { case _: Throwable =>
      Future.successful(InternalServerError("Unable to save data and get redirect link"))
    }

  private def updatePartners(
    eventualMaybePeoples: Option[Seq[ResponsiblePerson]],
    data: BusinessPartners
  ): Future[Option[Seq[ResponsiblePerson]]] =
    eventualMaybePeoples match {
      case Some(rpSeq) =>
        val updatedList = ResponsiblePerson.filter(rpSeq).map { responsiblePerson =>
          responsiblePerson.personName.exists(name => name.fullNameWithoutSpace.equals(data.value)) match {
            case true  =>
              val position = responsiblePerson.positions.fold[Option[Positions]](None)(p =>
                Some(Positions(p.positions + Partner, p.startDate))
              )
              responsiblePerson.copy(positions = position)
            case false => responsiblePerson
          }
        }
        Future.successful(Some(updatedList))
      case _           => Future.successful(eventualMaybePeoples)
    }
}
