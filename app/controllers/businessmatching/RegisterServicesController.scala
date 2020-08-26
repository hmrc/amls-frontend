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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RegisterServicesController @Inject()(authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val statusService: StatusService,
                                           val dataCacheConnector: DataCacheConnector,
                                           val businessMatchingService: BusinessMatchingService,
                                           val cc: MessagesControllerComponents,
                                           register_services: register_services) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        statusService.isPreSubmission(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap { isPreSubmission =>
          (for {
            businessMatching <- businessMatchingService.getModel(request.credId)
            businessActivities <- OptionT.fromOption[Future](businessMatching.activities)
          } yield {
            val form = Form2[BusinessMatchingActivities](businessActivities)
            val (newActivities, existing) = getActivityValues(form, isPreSubmission, Some(businessActivities.businessActivities))

            Ok(register_services(form, edit, sortActivities(newActivities), existing, isPreSubmission, businessMatching.preAppComplete))
          }) getOrElse {
            val (newActivities, existing) = getActivityValues(EmptyForm, isPreSubmission, None)
            Ok(register_services(EmptyForm, edit, sortActivities(newActivities), existing, isPreSubmission, showReturnLink = false))
          }
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        import jto.validation.forms.Rules._
        Form2[BusinessMatchingActivities](request.body) match {
          case invalidForm: InvalidForm =>
            statusService.isPreSubmission(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap { isPreSubmission =>
              (for {
                bm <- businessMatchingService.getModel(request.credId)
                businessActivities <- OptionT.fromOption[Future](bm.activities)
              } yield {
                businessActivities.businessActivities
              }).value map { activities =>
                val (newActivities, existing) = getActivityValues(
                  invalidForm,
                  isPreSubmission,
                  activities
                )
                BadRequest(
                  register_services(
                    invalidForm,
                    edit,
                    sortActivities(newActivities),
                    existing,
                    isPreSubmission
                  )
                )
              }
            }
          case ValidForm(_, data) =>
            businessActivities(request.amlsRefNumber, request.accountTypeId, request.credId, data) flatMap { ba =>
              getData[ResponsiblePerson](request.credId) flatMap { responsiblePeople =>

                val workFlow =
                  shouldPromptForApproval.tupled andThen
                    shouldPromptForFitAndProper.tupled

                val activities = ba._1
                val isRemovingActivity = ba._2

                val rps = responsiblePeople.map(rp => workFlow((rp, activities, isRemovingActivity)))

                updateResponsiblePeople(request.credId, rps) map { _ =>
                  redirectTo(data.businessActivities)
                }
              }
            }
        }
  }

  private def businessActivities(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String, data: BusinessMatchingActivities)
                        (implicit hc: HeaderCarrier) = {

    lazy val empty = BusinessMatchingActivities(Set())

    for {
      isPreSubmission <- statusService.isPreSubmission(amlsRegistrationNo, accountTypeId, credId)
      businessMatching <- businessMatchingService.getModel(credId).value
      businessActivitiesModel <- updateModel(
        credId,
        businessMatching,
        newModel(businessMatching.activities, data, isPreSubmission),
        isMsb(data, businessMatching.activities)
      )
      _ <- maybeRemoveAccountantForAMLSRegulations(credId, businessActivitiesModel)
      _ <- clearRemovedSections(
        credId,
        businessMatching.activities.getOrElse(empty).businessActivities,
        businessActivitiesModel.businessActivities
      )
      isRemovingActivity <- Future.successful(
        businessMatching.activities.getOrElse(empty).businessActivities > businessActivitiesModel.businessActivities
      )
    } yield (businessActivitiesModel, isRemovingActivity)
  }

  private def withoutAccountantForAMLSRegulations(activities: BusinessActivities): BusinessActivities =
    activities.whoIsYourAccountant(None)
      .accountantForAMLSRegulations(None)
      .taxMatters(None)
      .copy(hasAccepted = true)

  private def clearRemovedSections(credId: String,
                                   previousBusinessActivities: Set[BusinessActivity],
                                   currentBusinessActivities: Set[BusinessActivity]
                                  )(implicit hc: HeaderCarrier) = {
    for {
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, AccountancyServices)
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, EstateAgentBusinessService)
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, HighValueDealing)
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, MoneyServiceBusiness)
      _ <- clearSectionIfRemoved(credId, previousBusinessActivities, currentBusinessActivities, TrustAndCompanyServices)
      _ <- clearSupervisionIfNoLongerRequired(credId, previousBusinessActivities, currentBusinessActivities)
    } yield true
  }

  private def clearSectionIfRemoved(credId: String,
                                    previousBusinessActivities: Set[BusinessActivity],
                                    currentBusinessActivities: Set[BusinessActivity],
                                    businessActivity: BusinessActivity
                                   )(implicit hc: HeaderCarrier) = {
    if (previousBusinessActivities.contains(businessActivity) && !currentBusinessActivities.contains(businessActivity)) {
      businessMatchingService.clearSection(credId: String, businessActivity)
    } else {
      Future.successful(CacheMap)
    }
  }

  private def clearSupervisionIfNoLongerRequired(credId: String,
                                                 previousBusinessActivities: Set[BusinessActivity],
                                                 currentBusinessActivities: Set[BusinessActivity]
                                                )(implicit hc: HeaderCarrier) = {
    if (hasASPorTCSP(previousBusinessActivities) && !hasASPorTCSP(currentBusinessActivities)) {
      dataCacheConnector.save[Supervision](credId, Supervision.key, Supervision())
    } else {
      Future.successful(CacheMap)
    }
  }

  private def maybeRemoveAccountantForAMLSRegulations(credId: String, bmActivities: BusinessMatchingActivities)
                                                     (implicit hc: HeaderCarrier) = {
    for {
      activities <- dataCacheConnector.fetch[BusinessActivities](credId, BusinessActivities.key)
      strippedActivities <- Future.successful(withoutAccountantForAMLSRegulations(activities))
    } yield {
      if (bmActivities.hasBusinessOrAdditionalActivity(AccountancyServices) && activities.isDefined) {
        dataCacheConnector.save[BusinessActivities](credId, BusinessActivities.key, strippedActivities)
      } else {
        Future.successful(activities)
      }
    }
  }

  private def redirectTo(businessActivities: Set[BusinessActivity]) = if (businessActivities.contains(MoneyServiceBusiness)) {
    Redirect(routes.MsbSubSectorsController.get())
  } else {
    Redirect(routes.SummaryController.get())
  }

  private def getActivityValues(f: Form2[_], isPreSubmission: Boolean, existingActivities: Option[Set[BusinessActivity]]): (Set[String], Set[String]) = {

    val activities: Set[String] = Set(
      AccountancyServices,
      ArtMarketParticipant,
      BillPaymentServices,
      EstateAgentBusinessService,
      HighValueDealing,
      MoneyServiceBusiness,
      TrustAndCompanyServices,
      TelephonePaymentService
    ) map BusinessMatchingActivities.getValue

    existingActivities.fold[(Set[String], Set[String])]((activities, Set.empty)) { ea =>
      if (isPreSubmission) {
        (activities, Set.empty)
      } else {
        (activities diff (ea map BusinessMatchingActivities.getValue), activities intersect (ea map BusinessMatchingActivities.getValue))
      }
    }

  }

  private def newModel(existingActivities: Option[BusinessMatchingActivities],
                       added: BusinessMatchingActivities,
                       isPreSubmission: Boolean) = existingActivities.fold[BusinessMatchingActivities](added) { existing =>
    if (isPreSubmission) {
      added
    } else {
      BusinessMatchingActivities(existing.businessActivities, Some(added.businessActivities), existing.removeActivities, existing.dateOfChange)
    }
  }

  private def updateModel(credId: String,
                          businessMatching: BusinessMatching,
                          updatedBusinessActivities: BusinessMatchingActivities,
                          isMsb: Boolean)(implicit hc: HeaderCarrier): Future[BusinessMatchingActivities] = {

    val updatedBusinessMatching = isMsb match {
      case true =>
        businessMatching.activities(updatedBusinessActivities)
      case false =>
        businessMatching.activities(updatedBusinessActivities).copy(msbServices = None)
    }

    businessMatchingService.updateModel(credId, updatedBusinessMatching).value map { _ =>
      updatedBusinessActivities
    }

  }

  private def hasASPorTCSP(activities: Set[BusinessActivity]) = {
    val containsASP = activities.contains(AccountancyServices)
    val containsTCSP = activities.contains(TrustAndCompanyServices)
    containsASP | containsTCSP
  }

  private def isMsb(added: BusinessMatchingActivities, existing: Option[BusinessMatchingActivities]): Boolean =
    added.businessActivities.contains(MoneyServiceBusiness) | existing.fold(false)(act => act.businessActivities.contains(MoneyServiceBusiness))

  private def containsTcspOrMsb(activities: Set[BusinessActivity]) = (activities contains MoneyServiceBusiness) | (activities contains TrustAndCompanyServices)

  private def promptFitAndProper(rp: ResponsiblePerson) =
    rp.approvalFlags.hasAlreadyPassedFitAndProper.isEmpty

  private def removeFitAndProper(rp: ResponsiblePerson): ResponsiblePerson =
    rp.approvalFlags(rp.approvalFlags.copy(hasAlreadyPassedFitAndProper = None)).copy(hasAccepted = true)

  private def resetHasAccepted(rp: ResponsiblePerson): ResponsiblePerson =
    rp.approvalFlags.hasAlreadyPassedFitAndProper match {
      case None => rp.copy(hasAccepted = false)
      case _ => rp
    }

  private def updateResponsiblePeople(credId: String, responsiblePeople: Seq[ResponsiblePerson])(implicit hc: HeaderCarrier): Future[_] =
    dataCacheConnector.save[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key, responsiblePeople)

  val shouldPromptForFitAndProper: (ResponsiblePerson, BusinessMatchingActivities) => ResponsiblePerson =
    (rp, activities) => {
        if(promptFitAndProper(rp)) {
          resetHasAccepted(rp)
        } else {
          rp
        }
    }

  val shouldPromptForApproval: (ResponsiblePerson, BusinessMatchingActivities, Boolean) => (ResponsiblePerson, BusinessMatchingActivities) =
  (rp, activities, isRemoving) => {

    def approvalIsRequired(rp: ResponsiblePerson, businessActivities: BusinessMatchingActivities, isRemoving: Boolean) = {
      rp.approvalFlags.hasAlreadyPassedFitAndProper.contains(false) &
        !(containsTcspOrMsb(businessActivities.businessActivities)) &
        isRemoving
    }



    def setResponsiblePeopleForApproval(rp: ResponsiblePerson)
    : ResponsiblePerson = {
      (rp.approvalFlags.hasAlreadyPassedFitAndProper, rp.approvalFlags.hasAlreadyPaidApprovalCheck) match {
        case (Some(false), Some(_)) =>
          rp.approvalFlags(
            rp.approvalFlags.copy(
              hasAlreadyPaidApprovalCheck = None
            )
          ).copy(
            hasAccepted = false
          )
        case _ => rp
      }
    }

    if (approvalIsRequired(rp, activities, isRemoving)) {
      (setResponsiblePeopleForApproval(rp), activities)
    } else {
      (rp, activities)
    }
  }


  private def sortActivities(activities: Set[String]): Seq[String] = {
    (activities map BusinessMatchingActivities.getBusinessActivity).toSeq.sortBy(_.getMessage()) map BusinessMatchingActivities.getValue
  }
}
