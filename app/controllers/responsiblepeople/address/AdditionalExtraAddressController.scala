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
import forms.responsiblepeople.address.AdditionalExtraAddressFormProvider
import models.responsiblepeople._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.AdditionalExtraAddressView

import scala.concurrent.Future

class AdditionalExtraAddressController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: AdditionalExtraAddressFormProvider,
  view: AdditionalExtraAddressView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with AddressHelper {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.addressHistory) match {
            case (Some(name), Some(ResponsiblePersonAddressHistory(_, _, Some(additionalExtraAddress)))) =>
              Ok(view(formProvider().fill(additionalExtraAddress), edit, index, flow, name.titleName))
            case (Some(name), _)                                                                         =>
              Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _                                                                                       => NotFound(notFoundView)
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
                ResponsiblePersonAddress(modelFromPlayForm(formWithErrors), None),
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
    data: ResponsiblePersonAddress,
    credId: String,
    index: Int,
    edit: Boolean,
    flow: Option[String]
  ): Future[Result] =
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      (res.addressHistory, data.personAddress) match {
        case (Some(rph), _) if rph.additionalExtraAddress.isEmpty =>
          res.addressHistory(rph.copy(additionalExtraAddress = Some(data)))
        case (Some(rph), _: PersonAddressUK)
            if !ResponsiblePersonAddressHistory.isRPAddressInUK(rph.additionalExtraAddress) =>
          res.addressHistory(rph.copy(additionalExtraAddress = Some(data)))
        case (Some(rph), _: PersonAddressNonUK)
            if ResponsiblePersonAddressHistory.isRPAddressInUK(rph.additionalExtraAddress) =>
          res.addressHistory(rph.copy(additionalExtraAddress = Some(data)))
        case (_, _)                                               => res
      }
    } map { _ =>
      if (data.personAddress.isInstanceOf[PersonAddressUK]) {
        Redirect(routes.AdditionalExtraAddressUKController.get(index, edit, flow))
      } else {
        Redirect(routes.AdditionalExtraAddressNonUKController.get(index, edit, flow))
      }
    }
}
