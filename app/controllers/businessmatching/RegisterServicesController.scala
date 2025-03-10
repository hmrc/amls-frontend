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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.RegisterBusinessActivitiesFormProvider
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import services.cache.Cache
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.RegisterServicesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RegisterServicesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val dataCacheConnector: DataCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val cc: MessagesControllerComponents,
  formProvider: RegisterBusinessActivitiesFormProvider,
  view: RegisterServicesView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    statusService.isPreSubmission(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
      isPreSubmission =>
        (for {
          businessMatching   <- businessMatchingService.getModel(request.credId)
          businessActivities <- OptionT.fromOption[Future](businessMatching.activities)
          existing            = getActivityValues(isPreSubmission, Some(businessActivities.businessActivities))
          form                = formProvider().fill(businessActivities.businessActivities.toSeq)
        } yield Ok(view(form, edit, existing, isPreSubmission, businessMatching.preAppComplete))) getOrElse {
          Ok(
            view(
              formProvider(),
              edit,
              getActivityValues(isPreSubmission, None),
              isPreSubmission,
              showReturnLink = false
            )
          )
        }
    }
  }

  def post(edit: Boolean = false, includeCompanyNotRegistered: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            statusService.isPreSubmission(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
              isPreSubmission =>
                (for {
                  bm                 <- businessMatchingService.getModel(request.credId)
                  businessActivities <- OptionT.fromOption[Future](bm.activities)
                } yield businessActivities.businessActivities).value map { activities =>
                  BadRequest(
                    view(
                      formWithErrors,
                      edit,
                      getActivityValues(isPreSubmission, activities),
                      isPreSubmission
                    )
                  )
                }
            },
          value =>
            businessActivities(
              request.amlsRefNumber,
              request.accountTypeId,
              request.credId,
              new BusinessMatchingActivities(value.toSet)
            ) flatMap { ba =>
              getData[ResponsiblePerson](request.credId) flatMap { responsiblePeople =>
                val workFlow =
                  shouldPromptForApproval.tupled andThen
                    shouldPromptForFitAndProper.tupled

                val activities         = ba._1
                val isRemovingActivity = ba._2

                val rps = responsiblePeople.map(rp => workFlow((rp, activities, isRemovingActivity)))

                updateResponsiblePeople(request.credId, rps) map { _ =>
                  redirectTo(value.toSet, includeCompanyNotRegistered)
                }
              }
            }
        )
  }

  private def businessActivities(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    credId: String,
    data: BusinessMatchingActivities
  )(implicit hc: HeaderCarrier) = {

    lazy val empty = BusinessMatchingActivities(Set())

    for {
      isPreSubmission         <- statusService.isPreSubmission(amlsRegistrationNo, accountTypeId, credId)
      businessMatching        <- businessMatchingService.getModel(credId).value
      businessActivitiesModel <- updateModel(
                                   credId,
                                   businessMatching,
                                   newModel(businessMatching.activities, data, isPreSubmission),
                                   isMsb(data, businessMatching.activities)
                                 )
      _                       <- maybeRemoveAccountantForAMLSRegulations(credId, businessActivitiesModel)
      _                       <- clearRemovedSections(
                                   credId,
                                   businessMatching.activities.getOrElse(empty).businessActivities,
                                   businessActivitiesModel.businessActivities
                                 )
      isRemovingActivity      <-
        Future.successful(
          businessMatching.activities.getOrElse(empty).businessActivities > businessActivitiesModel.businessActivities
        )
    } yield (businessActivitiesModel, isRemovingActivity)
  }

  private def withoutAccountantForAMLSRegulations(activities: BusinessActivities): BusinessActivities =
    activities
      .whoIsYourAccountant(None)
      .accountantForAMLSRegulations(None)
      .taxMatters(None)
      .copy(hasAccepted = true)

  private def clearRemovedSections(
    credId: String,
    previousBusinessActivities: Set[BusinessActivity],
    currentBusinessActivities: Set[BusinessActivity]
  ) =
    for {
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, AccountancyServices)
      _ <-
        clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, EstateAgentBusinessService)
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, HighValueDealing)
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, MoneyServiceBusiness)
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, TrustAndCompanyServices)
      _ <- clearSupervisionIfNoLongerRequired(credId, previousBusinessActivities, currentBusinessActivities)
    } yield true

  private def clearSectionIfRemoved(
    credId: String,
    previousBusinessActivities: Set[BusinessActivity],
    currentBusinessActivities: Set[BusinessActivity],
    businessActivity: BusinessActivity
  ) =
    if (
      previousBusinessActivities.contains(businessActivity) && !currentBusinessActivities.contains(businessActivity)
    ) {
      businessMatchingService.clearSection(credId: String, businessActivity)
    } else {
      Future.successful(Cache)
    }

  private def clearSupervisionIfNoLongerRequired(
    credId: String,
    previousBusinessActivities: Set[BusinessActivity],
    currentBusinessActivities: Set[BusinessActivity]
  ) =
    if (hasASPorTCSP(previousBusinessActivities) && !hasASPorTCSP(currentBusinessActivities)) {
      dataCacheConnector.save[Supervision](credId, Supervision.key, Supervision())
    } else {
      Future.successful(Cache)
    }

  private def maybeRemoveAccountantForAMLSRegulations(credId: String, bmActivities: BusinessMatchingActivities) =
    for {
      activities         <- dataCacheConnector.fetch[BusinessActivities](credId, BusinessActivities.key)
      strippedActivities <- Future.successful(withoutAccountantForAMLSRegulations(activities))
    } yield
      if (bmActivities.hasBusinessOrAdditionalActivity(AccountancyServices) && activities.isDefined) {
        dataCacheConnector.save[BusinessActivities](credId, BusinessActivities.key, strippedActivities)
      } else {
        Future.successful(activities)
      }

  private def redirectTo(businessActivities: Set[BusinessActivity], includeCompanyNotRegistered: Boolean): Result =
    if (businessActivities.contains(MoneyServiceBusiness)) {
      Redirect(routes.MsbSubSectorsController.get())
    } else {
      if (includeCompanyNotRegistered) {
        Redirect(routes.CheckCompanyController.get())
      } else {
        Redirect(routes.SummaryController.get())
      }
    }

  def getActivityValues(
    isPreSubmission: Boolean,
    existingActivities: Option[Set[BusinessActivity]]
  ): Seq[BusinessActivity] =
    existingActivities.fold[Seq[BusinessActivity]](Seq.empty) { ea =>
      if (isPreSubmission) {
        Seq.empty
      } else {
        (BusinessMatchingActivities.all intersect ea).toSeq
      }
    }

  private def newModel(
    existingActivities: Option[BusinessMatchingActivities],
    added: BusinessMatchingActivities,
    isPreSubmission: Boolean
  ) = existingActivities.fold[BusinessMatchingActivities](added) { existing =>
    if (isPreSubmission) {
      added
    } else {
      BusinessMatchingActivities(
        existing.businessActivities,
        Some(added.businessActivities),
        existing.removeActivities,
        existing.dateOfChange
      )
    }
  }

  private def updateModel(
    credId: String,
    businessMatching: BusinessMatching,
    updatedBusinessActivities: BusinessMatchingActivities,
    isMsb: Boolean
  ): Future[BusinessMatchingActivities] = {

    val updatedBusinessMatching = if (isMsb) {
      businessMatching.activities(updatedBusinessActivities)
    } else {
      businessMatching.activities(updatedBusinessActivities).copy(msbServices = None)
    }

    businessMatchingService.updateModel(credId, updatedBusinessMatching).value map { _ =>
      updatedBusinessActivities
    }

  }

  private def hasASPorTCSP(activities: Set[BusinessActivity]) = {
    val containsASP  = activities.contains(AccountancyServices)
    val containsTCSP = activities.contains(TrustAndCompanyServices)
    containsASP | containsTCSP
  }

  private def isMsb(added: BusinessMatchingActivities, existing: Option[BusinessMatchingActivities]): Boolean =
    added.businessActivities.contains(MoneyServiceBusiness) | existing.fold(false)(act =>
      act.businessActivities.contains(MoneyServiceBusiness)
    )

  private def containsTcspOrMsb(activities: Set[BusinessActivity]) =
    (activities contains MoneyServiceBusiness) | (activities contains TrustAndCompanyServices)

  def promptFitAndProper(rp: ResponsiblePerson) =
    rp.approvalFlags.hasAlreadyPassedFitAndProper.isEmpty

  private def resetHasAccepted(rp: ResponsiblePerson): ResponsiblePerson =
    rp.approvalFlags.hasAlreadyPassedFitAndProper match {
      case None => rp.copy(hasAccepted = false)
      case _    => rp
    }

  private def updateResponsiblePeople(credId: String, responsiblePeople: Seq[ResponsiblePerson]): Future[_] =
    dataCacheConnector.save[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key, responsiblePeople)

  val shouldPromptForFitAndProper: (ResponsiblePerson, BusinessMatchingActivities) => ResponsiblePerson =
    (rp, _) =>
      if (promptFitAndProper(rp)) {
        resetHasAccepted(rp)
      } else {
        rp
      }

  val shouldPromptForApproval
    : (ResponsiblePerson, BusinessMatchingActivities, Boolean) => (ResponsiblePerson, BusinessMatchingActivities) =
    (rp, activities, isRemoving) => {

      def approvalIsRequired(
        rp: ResponsiblePerson,
        businessActivities: BusinessMatchingActivities,
        isRemoving: Boolean
      ) =
        rp.approvalFlags.hasAlreadyPassedFitAndProper.contains(false) &
          !containsTcspOrMsb(businessActivities.businessActivities) &
          isRemoving

      def setResponsiblePeopleForApproval(rp: ResponsiblePerson): ResponsiblePerson =
        (rp.approvalFlags.hasAlreadyPassedFitAndProper, rp.approvalFlags.hasAlreadyPaidApprovalCheck) match {
          case (Some(false), Some(_)) =>
            rp.approvalFlags(
              rp.approvalFlags.copy(
                hasAlreadyPaidApprovalCheck = None
              )
            ).copy(
              hasAccepted = false
            )
          case _                      => rp
        }

      if (approvalIsRequired(rp, activities, isRemoving)) {
        (setResponsiblePeopleForApproval(rp), activities)
      } else {
        (rp, activities)
      }
    }
}
