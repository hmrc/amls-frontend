/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.time_at_additional_extra_address

import scala.concurrent.Future

trait TimeAtAdditionalExtraAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>
      getData[ResponsiblePerson](index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,Some(ResponsiblePersonAddressHistory(_,_,Some(ResponsiblePersonAddress(_, Some(additionalExtraAddress))))),_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(time_at_additional_extra_address(Form2[TimeAtAddress](additionalExtraAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(time_at_additional_extra_address(Form2(DefaultAddressHistory), edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request => {
      (Form2[TimeAtAddress](request.body) match {
        case f: InvalidForm =>
          getData[ResponsiblePerson](index) map { rp =>
            BadRequest(time_at_additional_extra_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
        case ValidForm(_, data) =>
          getData[ResponsiblePerson](index) flatMap { responsiblePerson =>
            (for {
              rp <- responsiblePerson
              addressHistory <- rp.addressHistory
              additionalExtraAddress <- addressHistory.additionalExtraAddress
            } yield {
              val additionalExtraAddressWithTime = additionalExtraAddress.copy(
                timeAtAddress = Some(data)
              )
              updateAndRedirect(additionalExtraAddressWithTime, index, edit, flow)
            }) getOrElse Future.successful(NotFound(notFoundView))
          }
      }).recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
    }
  }

  private def updateAndRedirect
  (data: ResponsiblePersonAddress, index: Int, edit: Boolean, flow: Option[String])
  (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePerson](index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) => a.additionalExtraAddress(data)
          case _ => ResponsiblePersonAddressHistory(additionalExtraAddress = Some(data))
        }
      )
    } map { _ =>
      edit match {
        case true => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
        case false => Redirect(routes.PositionWithinBusinessController.get(index, edit, flow))
      }
    }
  }
}

object TimeAtAdditionalExtraAddressController extends TimeAtAdditionalExtraAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
