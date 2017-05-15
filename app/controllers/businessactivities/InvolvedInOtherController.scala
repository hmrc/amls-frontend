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

package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import models.businessmatching._
import play.api.i18n.Messages
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.businessactivities._

import scala.concurrent.Future

trait InvolvedInOtherController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  implicit val statusService: StatusService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              } yield {
                (for {
                  businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
                  involvedInOther <- businessActivities.involvedInOther
                } yield Ok(involved_in_other_name(Form2[InvolvedInOther](involvedInOther),
                  edit, businessMatching, businessTypes(businessMatching))))
                  .getOrElse(Ok(involved_in_other_name(EmptyForm, edit, businessMatching, businessTypes(businessMatching))))
              }) getOrElse Ok(involved_in_other_name(EmptyForm, edit, None, None))
          }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[InvolvedInOther](request.body) match {
        case f: InvalidForm =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
          } yield businessMatching match {
            case Some(x) => BadRequest(involved_in_other_name(f, edit, None, businessTypes(businessMatching)))
            case None => BadRequest(involved_in_other_name(f, edit, None, None))
          }
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key, getUpdatedBA(businessActivities, data))

          } yield redirectDependingOnResponse(data, edit)
      }
    }
  }

  private def getUpdatedBA(businessActivities: Option[BusinessActivities], data: InvolvedInOther): BusinessActivities = {
    (businessActivities, data) match {
      case (Some(ba), InvolvedInOtherYes(_)) => ba.copy(involvedInOther = Some(data))
      case (Some(ba), InvolvedInOtherNo) => ba.copy(involvedInOther = Some(data), expectedBusinessTurnover = None)
      case (_, _) => BusinessActivities(involvedInOther = Some(data))
    }
  }

  private def redirectDependingOnResponse(data: InvolvedInOther, edit: Boolean) = data match {
    case InvolvedInOtherYes(_) => Redirect(routes.ExpectedBusinessTurnoverController.get(edit))
    case InvolvedInOtherNo => edit match {
      case false => Redirect(routes.ExpectedAMLSTurnoverController.get(edit))
      case true => Redirect(routes.SummaryController.get())
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

    typesString match {
      case Some(types) => Some(typesString.getOrElse(Set()).mkString(", ") + ".")
      case None => None
    }

  }


}

object InvolvedInOtherController extends InvolvedInOtherController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override implicit val statusService: StatusService = StatusService
}
