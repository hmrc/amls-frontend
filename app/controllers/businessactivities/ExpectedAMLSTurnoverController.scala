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

package controllers.businessactivities

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, ExpectedAMLSTurnover}
import models.businessmatching._
import play.api.i18n.Messages
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.businessactivities._

import scala.concurrent.Future

class ExpectedAMLSTurnoverController @Inject() (val dataCacheConnector: DataCacheConnector,
                                                override val authConnector: AuthConnector,
                                                implicit val statusService: StatusService
                                               )extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true => dataCacheConnector.fetchAll map {
          optionalCache =>
            (for {
              cache <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {
              (for {
                businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
                expectedTurnover <- businessActivities.expectedAMLSTurnover
              } yield Ok(expected_amls_turnover(Form2[ExpectedAMLSTurnover](expectedTurnover), edit, businessMatching, businessTypes(businessMatching))))
                .getOrElse (Ok(expected_amls_turnover(EmptyForm, edit, businessMatching, businessTypes(businessMatching))))
            }) getOrElse Ok(expected_amls_turnover(EmptyForm, edit, None, None))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedAMLSTurnover](request.body) match {
        case f: InvalidForm =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
          } yield {
            BadRequest(expected_amls_turnover(f, edit, None, businessTypes(businessMatching)))
          }

        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.expectedAMLSTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.BusinessFranchiseController.get())
          }
      }
    }
  }

  private def businessTypes(activities: BusinessMatching): Option[String] = {
    val typesString = activities.activities map { a =>
      a.businessActivities.map { line =>
        line match {
          case AccountancyServices => Messages("businessmatching.registerservices.servicename.lbl.01")
          case BillPaymentServices => Messages("businessmatching.registerservices.servicename.lbl.02")
          case EstateAgentBusinessService => Messages("businessmatching.registerservices.servicename.lbl.03")
          case HighValueDealing => Messages("businessmatching.registerservices.servicename.lbl.04")
          case MoneyServiceBusiness => Messages("businessmatching.registerservices.servicename.lbl.05")
          case TrustAndCompanyServices => Messages("businessmatching.registerservices.servicename.lbl.06")
          case TelephonePaymentService => Messages("businessmatching.registerservices.servicename.lbl.07")
        }
      }
    }

    val blah = typesString match {
      case Some(types) => Some(typesString.getOrElse(List()).toList.sorted.mkString("|"))
      case None => None
    }
  }
}
