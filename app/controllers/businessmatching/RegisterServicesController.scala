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
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessActivities, _}
import models.status.{NotCompleted, SubmissionReady, SubmissionStatus}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching._

import scala.concurrent.Future

@Singleton
class RegisterServicesController @Inject()(val authConnector: AuthConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService)() extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getStatus flatMap { status =>
          businessMatchingService.getModel.value map { bm =>
            (for {
              businessMatching <- bm
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
            statusService.getStatus flatMap { status =>
              (for {
                bm <- businessMatchingService.getModel
                businessActivities <- OptionT.fromOption[Future](bm.activities)
              } yield {
                businessActivities.businessActivities
              }).value map { activities =>
                val (newActivities, existing) = getActivityValues(invalidForm, status, activities)
                BadRequest(register_services(invalidForm, edit, newActivities, existing, status))
              }
            }
          case ValidForm(_, data) =>
            for {
              isPreSubmission <- statusService.isPreSubmission
              businessMatching <- businessMatchingService.getModel.value
              _ <- isMsb(data, businessMatching.activities) match {
                case true =>
                  businessMatchingService.updateModel(
                    businessMatching.activities(updateModel(businessMatching.activities, data, isPreSubmission))
                  ).value
                case false =>
                  businessMatchingService.updateModel(
                    businessMatching.activities(updateModel(businessMatching.activities, data, isPreSubmission)).copy(msbServices = None)
                  ).value
                }
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

    existingActivities.fold[(Set[String], Set[String])]((activities, Set.empty)) { ea =>
      status match {
        case SubmissionReady | NotCompleted => (activities, Set.empty)
        case _ => (activities diff (ea map BusinessActivities.getValue), activities intersect (ea map BusinessActivities.getValue))
      }
    }

  }

  private def updateModel(existing: Option[BusinessActivities], added: BusinessActivities, isPreSubmission: Boolean): BusinessActivities =
    existing.fold[BusinessActivities](added) { existing =>
      if(isPreSubmission){
        added
      } else {
        BusinessActivities(existing.businessActivities, Some(added.businessActivities), existing.removeActivities, existing.dateOfChange)
      }
    }

  private def isMsb(added: BusinessActivities, existing: Option[BusinessActivities]): Boolean =
    added.businessActivities.contains(MoneyServiceBusiness) | existing.fold(false)(act => act.businessActivities.contains(MoneyServiceBusiness))

}