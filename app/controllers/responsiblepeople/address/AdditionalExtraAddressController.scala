/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import services.AutoCompleteService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, ControllerHelper}
import views.html.responsiblepeople.address.additional_extra_address

import scala.concurrent.Future

class AdditionalExtraAddressController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                 authAction: AuthAction,
                                                 autoCompleteService: AutoCompleteService) extends AddressHelper with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, Some(ResponsiblePersonAddressHistory(_, _, Some(additionalExtraAddress))), _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(additional_extra_address(Form2[ResponsiblePersonAddress](additionalExtraAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(additional_extra_address(EmptyForm, edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        (Form2[ResponsiblePersonAddress](request.body)(ResponsiblePersonAddress.addressFormRule(PersonAddress.formRule(AddressType.OtherPrevious))) match {
          case f: InvalidForm if f.data.get("isUK").isDefined
          => processForm(ResponsiblePersonAddress(modelFromForm(f), None), request.credId, index, edit, flow)
          case f: InvalidForm
          => getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(additional_extra_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data)
          => processForm(data, request.credId, index, edit, flow)
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }

  private def processForm(data: ResponsiblePersonAddress, credId: String, index: Int, edit: Boolean, flow: Option[String])
                         (implicit hc: HeaderCarrier) = {

    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      (res.addressHistory, data.personAddress) match {
        case (Some(rph), _) if rph.additionalExtraAddress.isEmpty
        => res.addressHistory(rph.copy(additionalExtraAddress = Some(data)))
        case (Some(rph), _: PersonAddressUK) if !ResponsiblePersonAddressHistory.isRPAddressInUK(rph.additionalExtraAddress)
        => res.addressHistory(rph.copy(additionalExtraAddress = Some(data)))
        case (Some(rph), _: PersonAddressNonUK) if ResponsiblePersonAddressHistory.isRPAddressInUK(rph.additionalExtraAddress)
        => res.addressHistory(rph.copy(additionalExtraAddress = Some(data)))
        case (_, _) => res
      }
    } map { _ =>
      if (data.personAddress.isInstanceOf[PersonAddressUK]) {
        Redirect(routes.AdditionalExtraAddressUKController.get(index, edit, flow))
      } else {
        Redirect(routes.AdditionalExtraAddressNonUKController.get(index, edit, flow))
      }
    }
  }
}