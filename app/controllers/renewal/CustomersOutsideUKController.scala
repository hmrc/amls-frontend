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

package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.renewal.{CustomersOutsideUK, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.renewal._

import scala.concurrent.Future

@Singleton
class CustomersOutsideUKController @Inject()(val dataCacheConnector: DataCacheConnector,
                                             val authConnector: AuthConnector,
                                             val renewalService: RenewalService
                                            ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        renewalService.getRenewal map {
          response =>
            val form: Form2[CustomersOutsideUK] = (for {
              renewal <- response
              customers <- renewal.customersOutsideUK
            } yield Form2[CustomersOutsideUK](customers)).getOrElse(EmptyForm)
            Ok(customers_outside_uk(form, edit))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[CustomersOutsideUK](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(customers_outside_uk(f, edit)))
          case ValidForm(_, data) => {
            dataCacheConnector.fetchAll flatMap { optionalCache =>
              (for {
                cache <- optionalCache
                businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                renewal <- cache.getEntry[Renewal](Renewal.key)
              } yield {
                renewalService.updateRenewal({
                  if (ControllerHelper.hasCustomersOutsideUK(renewal.customersOutsideUK) && !ControllerHelper.hasCustomersOutsideUK(Some(data))) {
                    renewal.customersOutsideUK(data).copy(sendTheLargestAmountsOfMoney = None, mostTransactions = None)
                  } else {
                    renewal.customersOutsideUK(data)
                  }
                }) map { _ =>
                  redirect(edit, data, renewal, businessMatching)
                }
              }) getOrElse Future.successful(Redirect(routes.SummaryController.get()))
            }
          }
        }
  }

  private def redirect(edit: Boolean, data: CustomersOutsideUK, renewal: Renewal, businessMatching: BusinessMatching) = {
    edit match {
      case true => if (
        msbServicesContainsTransmittingMoney(businessMatching.msbServices) &&
          !ControllerHelper.hasCustomersOutsideUK(renewal.customersOutsideUK) &&
          ControllerHelper.hasCustomersOutsideUK(Some(data))
      ) {
        Redirect(routes.SendTheLargestAmountsOfMoneyController.get())
      } else {
        Redirect(routes.SummaryController.get())
      }
      case false => redirectDependingOnActivities(businessMatching)
    }
  }

  private def redirectDependingOnActivities(businessMatching: BusinessMatching) = {
    ControllerHelper.getBusinessActivity(Some(businessMatching)) match {
      case Some(activities) if activities.businessActivities contains MoneyServiceBusiness => Redirect(routes.TotalThroughputController.get())
      case Some(activities) if activities.businessActivities contains HighValueDealing => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
      case _ => Redirect(routes.SummaryController.get())
    }
  }

  private def msbServicesContainsTransmittingMoney(msbServices: Option[MsbServices]): Boolean = {
    msbServices match {
      case Some(services) => services.msbServices.contains(TransmittingMoney)
      case _ => false
    }
  }

}


