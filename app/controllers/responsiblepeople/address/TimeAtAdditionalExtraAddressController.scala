/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.responsiblepeople.address

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.address.TimeAtAddressFormProvider
import models.responsiblepeople._
import play.api.mvc._
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.TimeAtAdditionalExtraAddressView

import scala.concurrent.Future

class TimeAtAdditionalExtraAddressController @Inject() (val dataCacheConnector: DataCacheConnector,
                                                        authAction: AuthAction,
                                                        val ds: CommonPlayDependencies,
                                                        val cc: MessagesControllerComponents,
                                                        formProvider: TimeAtAddressFormProvider,
                                                        view: TimeAtAdditionalExtraAddressView,
                                                        implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, Some(ResponsiblePersonAddressHistory(_, _, Some(ResponsiblePersonAddress(_, Some(additionalExtraAddress))))), _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(view(formProvider().fill(additionalExtraAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(view(formProvider(), edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          },
        data =>
          getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
            (for {
              rp <- responsiblePerson
              addressHistory <- rp.addressHistory
              additionalExtraAddress <- addressHistory.additionalExtraAddress
            } yield {
              val additionalExtraAddressWithTime = additionalExtraAddress.copy(
                timeAtAddress = Some(data)
              )
              updateAndRedirect(request.credId, additionalExtraAddressWithTime, index, edit, flow)
            }) getOrElse Future.successful(NotFound(notFoundView))
          }
      ).recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
  }

  private def updateAndRedirect(credId: String, data: ResponsiblePersonAddress, index: Int, edit: Boolean, flow: Option[String])
                               (implicit request: Request[AnyContent]): Future[Result] = {
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) => a.additionalExtraAddress(data)
          case _ => ResponsiblePersonAddressHistory(additionalExtraAddress = Some(data))
        }
      )
    } map { _ =>
      if (edit) {
        Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index, flow))
      } else {
        Redirect(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(index, edit, flow))
      }
    }
  }
}