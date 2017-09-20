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

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.status.{NotCompleted, SubmissionReady, SubmissionStatus}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching._

@Singleton
class RegisterServicesController @Inject()(val authConnector: AuthConnector,
                                           val dataCacheConnector: DataCacheConnector,
                                           val statusService: StatusService)() extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getStatus flatMap { status =>
          dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
            response =>
              (for {
                businessMatching <- response
                businessActivities <- businessMatching.activities
              } yield {
                val form = Form2[BusinessActivities](businessActivities)
                Ok(register_services(form, edit, getActivityValues(form, status, Some(businessActivities.businessActivities))))
              }) getOrElse Ok(register_services(EmptyForm, edit, getActivityValues(EmptyForm, status, None)))
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
              BadRequest(register_services(invalidForm, edit, getActivityValues(invalidForm, status, None)))
            }
          case ValidForm(_, data) =>
            for {
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
              _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
                data.businessActivities.contains(MoneyServiceBusiness) match {
                  case false => businessMatching.copy(activities = Some(data), msbServices = None)
                  case true => businessMatching.activities(data)
                }
              )
            } yield data.businessActivities.contains(MoneyServiceBusiness) match {
              case true => Redirect(routes.ServicesController.get(false))
              case false => Redirect(routes.SummaryController.get())
            }
        }
  }

  private def getActivityValues(f: Form2[_], status: SubmissionStatus, existingActivities: Option[Set[BusinessActivity]]): Set[String] = {

    val activities: Set[BusinessActivity] = Set(
      AccountancyServices,
      BillPaymentServices,
      EstateAgentBusinessService,
      HighValueDealing,
      MoneyServiceBusiness,
      TrustAndCompanyServices,
      TelephonePaymentService
    )

    existingActivities.fold[Set[BusinessActivity]](activities){ ea =>
      status match {
        case SubmissionReady | NotCompleted => activities
        case _ => activities diff ea
      }
    } map BusinessActivities.getValue

  }

}