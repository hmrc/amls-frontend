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

package controllers.businessmatching

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import services.businessmatching.BusinessMatchingService
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessActivities, _}
import models.status.{NotCompleted, SubmissionReady, SubmissionStatus}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching._

@Singleton
class RegisterServicesController @Inject()(val authConnector: AuthConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService)() extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getStatus flatMap { status =>
          businessMatchingService.getModel.value map {
            response =>
              (for {
                businessMatching <- response
                businessActivities <- businessMatching.activities
              } yield {
                val form = Form2[BusinessActivities](businessActivities)
                val (newActivities, existing) = getActivityValues(form, status, Some(businessActivities.businessActivities))
                Ok(register_services(form, edit, newActivities, existing, status))
              }) getOrElse {
                val (newActivities, existing) = getActivityValues(EmptyForm, status, None)
                Ok(register_services(EmptyForm, edit, newActivities, existing, status))
              }
          }
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        import jto.validation.forms.Rules._
        Form2[BusinessActivities](request.body) match {
          case invalidForm: InvalidForm =>
            statusService.getStatus map { status =>
              val (newActivities, existing) = getActivityValues(invalidForm, status, None)
              BadRequest(register_services(invalidForm, edit, newActivities, existing, status))
            }
          case ValidForm(_, data) =>
            for {
              businessMatching <- businessMatchingService.getModel.value
              _ <- businessMatchingService.updateModel(
                data.businessActivities.contains(MoneyServiceBusiness) match {
                  case false => businessMatching.copy(activities = Some(data), msbServices = None)
                  case true => businessMatching.activities(data)
                }
              ).value
            } yield data.businessActivities.contains(MoneyServiceBusiness) match {
              case true => Redirect(routes.ServicesController.get(false))
              case false => Redirect(routes.SummaryController.get())
            }
        }
  }

  private def getActivityValues(f: Form2[_], status: SubmissionStatus, existingActivities: Option[Set[BusinessActivity]]): (Set[String], Set[String]) = {

    val activities: Set[String] = Set(
      AccountancyServices,
      BillPaymentServices,
      EstateAgentBusinessService,
      HighValueDealing,
      MoneyServiceBusiness,
      TrustAndCompanyServices,
      TelephonePaymentService
    ) map BusinessActivities.getValue

    existingActivities.fold[(Set[String], Set[String])]((activities, Set.empty)){ ea =>
      status match {
        case SubmissionReady | NotCompleted => (activities, Set.empty)
        case _ => (activities diff(ea map BusinessActivities.getValue), activities intersect(ea map BusinessActivities.getValue))
      }
    }

  }

  private def updateModel(existingServices: Set[BusinessActivity], addedServices: Set[BusinessActivity], status: SubmissionStatus): Set[BusinessActivity] = {

    status match {
      case NotCompleted | SubmissionReady => addedServices
      case _ => existingServices ++ addedServices
    }
  }

}