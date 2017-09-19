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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import play.twirl.api.HtmlFormat
import views.html.businessmatching._
import views.html.include.forms2.checkbox

import scala.concurrent.Future

trait RegisterServicesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
          response =>
            (for {
              businessMatching <- response
              businessActivities <- businessMatching.activities
            } yield {
              val form = Form2[BusinessActivities](businessActivities)
              Ok(register_services(form, edit, inc(form, Some(businessActivities.businessActivities))))
            }) getOrElse Ok(register_services(EmptyForm, edit, inc(EmptyForm, None)))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        import jto.validation.forms.Rules._
        Form2[BusinessActivities](request.body) match {
          case invalidForm: InvalidForm =>
            Future.successful(BadRequest(register_services(invalidForm, edit, inc(invalidForm, None))))
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

  def inc(f: Form2[_], existingActivities: Option[Set[BusinessActivity]]) = {

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
      activities.intersect(ea)
    } map { ba =>
      val value = "02"//businessActivities.getValue(ba)
      checkbox(
        f = f("businessActivities[]"),
        labelText = s"businessmatching.registerservices.servicename.lbl.$value",
        value = value
      )
    }

  }

}

object RegisterServicesController extends RegisterServicesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}