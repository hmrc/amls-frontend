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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import play.api.mvc.MessagesControllerComponents
import services.AutoCompleteService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.additional_extra_address_NonUK

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdditionalExtraAddressNonUKController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                      authAction: AuthAction,
                                                      implicit val auditConnector: AuditConnector,
                                                      autoCompleteService: AutoCompleteService,
                                                      val ds: CommonPlayDependencies,
                                                      val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection with AddressHelper {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, Some(ResponsiblePersonAddressHistory(_, _, Some(additionalExtraAddress))), _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(additional_extra_address_NonUK(Form2[ResponsiblePersonAddress](additionalExtraAddress), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(additional_extra_address_NonUK(EmptyForm, edit, index, flow, personName.titleName, autoCompleteService.getCountries))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        (Form2[ResponsiblePersonAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(additional_extra_address_NonUK(f, edit, index, flow, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
            }
          case ValidForm(_, data) => {
            getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
              (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                additionalExtraAddress <- addressHistory.additionalExtraAddress
              } yield {
                val additionalExtraAddressWithTime = data.copy(timeAtAddress = additionalExtraAddress.timeAtAddress)
                updateAdditionalExtraAddressAndRedirect(request.credId, additionalExtraAddressWithTime, index, edit, flow)
              }) getOrElse updateAdditionalExtraAddressAndRedirect(request.credId, data, index, edit, flow)
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }
}