/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.responsiblepeople.address.CurrentAddressFormProvider
import models.responsiblepeople._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.{AuthAction, ControllerHelper, DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.address.CurrentAddressView

import scala.concurrent.Future

class CurrentAddressController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: CurrentAddressFormProvider,
  view: CurrentAddressView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with DateOfChangeHelper
    with AddressHelper {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.addressHistory) match {
            case (Some(name), Some(ResponsiblePersonAddressHistory(Some(currentAddress), _, _))) =>
              Ok(view(formProvider().fill(currentAddress), edit, index, flow, name.titleName))
            case (Some(name), _)                                                                 => Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _                                                                               => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] =
    authAction.async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            if (formWithErrors.data.contains("isUK")) {
              processForm(
                ResponsiblePersonCurrentAddress(modelFromPlayForm(formWithErrors), None, None),
                request.credId,
                index,
                edit,
                flow
              )
            } else {
              getData[ResponsiblePerson](request.credId, index) map { rp =>
                BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp)))
              }
            },
          data => processForm(data, request.credId, index, edit, flow)
        )
        .recoverWith { case _: IndexOutOfBoundsException =>
          Future.successful(NotFound(notFoundView))
        }
    }

  private def processForm(
    data: ResponsiblePersonCurrentAddress,
    credId: String,
    index: Int,
    edit: Boolean,
    flow: Option[String]
  ): Future[Result] =
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      (res.addressHistory, data.personAddress) match {
        case (None, _) => res.addressHistory(ResponsiblePersonAddressHistory(Some(data)))
        case (Some(rph), addrUk: PersonAddressUK)
            if !ResponsiblePersonAddressHistory.isRPCurrentAddressInUK(rph.currentAddress) =>
          res.addressHistory(
            rph.copy(currentAddress =
              Some(ResponsiblePersonCurrentAddress(addrUk, rph.currentAddress.flatMap(_.timeAtAddress)))
            )
          )
        case (Some(rph), addrNonUK: PersonAddressNonUK)
            if ResponsiblePersonAddressHistory.isRPCurrentAddressInUK(rph.currentAddress) =>
          res.addressHistory(
            rph.copy(currentAddress =
              Some(ResponsiblePersonCurrentAddress(addrNonUK, rph.currentAddress.flatMap(_.timeAtAddress)))
            )
          )
        case (_, _)    => res
      }
    } map { _ =>
      if (data.personAddress.isInstanceOf[PersonAddressUK]) {
        Redirect(routes.CurrentAddressUKController.get(index, edit, flow))
      } else {
        Redirect(routes.CurrentAddressNonUKController.get(index, edit, flow))
      }
    }
}
