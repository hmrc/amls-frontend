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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import config.AppConfig
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}

import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.moneyservicebusiness.{MoneyServiceBusiness => MSBModel}
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import models.supervision.Supervision
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching._

import scala.concurrent.Future

@Singleton
class RegisterServicesController @Inject()(val authConnector: AuthConnector,
                                           val statusService: StatusService,
                                           val dataCacheConnector: DataCacheConnector,
                                           val businessMatchingService: BusinessMatchingService,
                                           val appConfig:AppConfig)() extends BaseController with RepeatingSection {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.isPreSubmission flatMap { isPreSubmission =>
          (for {
            businessMatching <- businessMatchingService.getModel
            businessActivities <- OptionT.fromOption[Future](businessMatching.activities)
          } yield {
            val form = Form2[BusinessMatchingActivities](businessActivities)
            val (newActivities, existing) = getActivityValues(form, isPreSubmission, Some(businessActivities.businessActivities))

            Ok(register_services(form, edit, newActivities, existing, isPreSubmission, businessMatching.preAppComplete))
          }) getOrElse {
            val (newActivities, existing) = getActivityValues(EmptyForm, isPreSubmission, None)
            Ok(register_services(EmptyForm, edit, newActivities, existing, isPreSubmission, showReturnLink = false))
          }
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        import jto.validation.forms.Rules._
        Form2[BusinessMatchingActivities](request.body) match {
          case invalidForm: InvalidForm =>
            statusService.isPreSubmission flatMap { isPreSubmission =>
              (for {
                bm <- businessMatchingService.getModel
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
                    newActivities,
                    existing,
                    isPreSubmission
                  )
                )
              }
            }
          case ValidForm(_, data) =>
            (for {
              isPreSubmission <- statusService.isPreSubmission
              businessMatching <- businessMatchingService.getModel.value
              savedModel <- updateModel(
                businessMatching,
                newModel(businessMatching.activities,
                  data,
                  isPreSubmission
                ),
                isMsb(data, businessMatching.activities)
              )
              _ <- maybeRemoveAccountantForAMLSRegulations(savedModel)
              _ <- clearRemovedSections(
                businessMatching.activities.getOrElse(
                  BusinessMatchingActivities(
                    Set()
                  )
                ).businessActivities,
                savedModel.businessActivities
              )
            } yield savedModel) flatMap { savedActivities =>
              getData[ResponsiblePerson] flatMap { responsiblePeople =>

                val workFlow =
                  shouldPromptForApproval.tupled andThen
                  shouldPromptForFitAndProper.tupled

                val rps = responsiblePeople.map(rp => workFlow((rp, savedActivities)))

                updateResponsiblePeople(rps) map { _ =>
                  redirectTo(data.businessActivities)
                }
              }
            }
        }
  }

  private def withoutAccountantForAMLSRegulations(activities: BusinessActivities): BusinessActivities =
    activities.whoIsYourAccountant(None)
      .accountantForAMLSRegulations(None)
      .taxMatters(None)
      .copy(hasAccepted = true)

  private def clearRemovedSections(previousBusinessActivities: Set[BusinessActivity],
                                   currentBusinessActivities: Set[BusinessActivity]
                                  )(implicit ac: AuthContext, hc: HeaderCarrier) = {
    for {
      _ <- clearSectionIfRemoved(previousBusinessActivities, currentBusinessActivities, AccountancyServices)
      _ <- clearSectionIfRemoved(previousBusinessActivities, currentBusinessActivities, EstateAgentBusinessService)
      _ <- clearSectionIfRemoved(previousBusinessActivities, currentBusinessActivities, HighValueDealing)
      _ <- clearSectionIfRemoved(previousBusinessActivities, currentBusinessActivities, MoneyServiceBusiness)
      _ <- clearSectionIfRemoved(previousBusinessActivities, currentBusinessActivities, TrustAndCompanyServices)
      _ <- clearSupervisionIfNoLongerRequired(previousBusinessActivities, currentBusinessActivities)
    } yield true
  }

  private def clearSectionIfRemoved(previousBusinessActivities: Set[BusinessActivity],
                                    currentBusinessActivities: Set[BusinessActivity],
                                    businessActivity: BusinessActivity
                                   )(implicit ac: AuthContext, hc: HeaderCarrier) = {
    if (previousBusinessActivities.contains(businessActivity) && !currentBusinessActivities.contains(businessActivity)) {
      businessMatchingService.clearSection(businessActivity)
    } else {
      Future.successful(CacheMap)
    }
  }

  private def clearSupervisionIfNoLongerRequired(previousBusinessActivities: Set[BusinessActivity],
                                                 currentBusinessActivities: Set[BusinessActivity]
                                                )(implicit ac: AuthContext, hc: HeaderCarrier) = {
    if (hasASPorTCSP(previousBusinessActivities) && !hasASPorTCSP(currentBusinessActivities)) {
      dataCacheConnector.save[Supervision](Supervision.key, Supervision())
    } else {
      Future.successful(CacheMap)
    }
  }

  private def maybeRemoveAccountantForAMLSRegulations(bmActivities: BusinessMatchingActivities)
                                                     (implicit ac: AuthContext, hc: HeaderCarrier) = {
    for {
      activities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
      strippedActivities <- Future.successful(withoutAccountantForAMLSRegulations(activities))
    } yield {
      if (bmActivities.hasBusinessOrAdditionalActivity(AccountancyServices) && activities.isDefined) {
        dataCacheConnector.save[BusinessActivities](BusinessActivities.key, strippedActivities)
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

  private def updateModel(businessMatching: BusinessMatching,
                          updatedBusinessActivities: BusinessMatchingActivities,
                          isMsb: Boolean)(implicit ac: AuthContext, hc: HeaderCarrier): Future[BusinessMatchingActivities] = {

    val updatedBusinessMatching = isMsb match {
      case true =>
        businessMatching.activities(updatedBusinessActivities)
      case false =>
        businessMatching.activities(updatedBusinessActivities).copy(msbServices = None)
    }

    businessMatchingService.updateModel(updatedBusinessMatching).value map { _ =>
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

  private def fitAndProperRequired(businessActivities: BusinessMatchingActivities): Boolean = {
    if (!appConfig.phase2ChangesToggle) {

      (businessActivities.businessActivities, businessActivities.additionalActivities) match {
        case (a, Some(e)) => containsTcspOrMsb(a) | containsTcspOrMsb(e)
        case (a, _) => containsTcspOrMsb(a)
      }
    } else {
      true
    }
  }

  private def promptFitAndProper(rp: ResponsiblePerson) =
    rp.approvalFlags.hasAlreadyPassedFitAndProper.isEmpty

  private def removeFitAndProper(rp: ResponsiblePerson): ResponsiblePerson =
    rp.approvalFlags(rp.approvalFlags.copy(hasAlreadyPassedFitAndProper = None)).copy(hasAccepted = true)

  private def resetHasAccepted(rp: ResponsiblePerson): ResponsiblePerson =
    rp.approvalFlags.hasAlreadyPassedFitAndProper match {
      case None => rp.copy(hasAccepted = false)
      case _ => rp
    }

  private def updateResponsiblePeople(responsiblePeople: Seq[ResponsiblePerson])(implicit ac: AuthContext, hc: HeaderCarrier): Future[_] =
    dataCacheConnector.save[Seq[ResponsiblePerson]](ResponsiblePerson.key, responsiblePeople)

  val shouldPromptForFitAndProper:
    (ResponsiblePerson, BusinessMatchingActivities) => ResponsiblePerson =
    (rp, activities) => {

      if(fitAndProperRequired(activities)) {
        if(promptFitAndProper(rp)) {
          resetHasAccepted(rp)
        } else {
          rp
        }
      } else {
        removeFitAndProper(rp)
      }
    }

  val shouldPromptForApproval:
  (ResponsiblePerson, BusinessMatchingActivities) => (ResponsiblePerson, BusinessMatchingActivities) =
  (rp, activities) => {

    def approvalIsRequired(rp: ResponsiblePerson, businessActivities: BusinessMatchingActivities) =
      rp.approvalFlags.hasAlreadyPassedFitAndProper.contains(false) &
      !(containsTcspOrMsb(businessActivities.businessActivities))

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

    if (approvalIsRequired(rp, activities)) {
      (setResponsiblePeopleForApproval(rp), activities)
    } else {
      (rp, activities)
    }
  }
}
