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

import com.google.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import play.api.mvc.MessagesControllerComponents
import services.AutoCompleteService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.additional_address_UK


import scala.concurrent.Future

@Singleton
class AdditionalAddressUKController @Inject()(override val dataCacheConnector: DataCacheConnector,
                                              authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              implicit val auditConnector: AuditConnector,
                                              val autoCompleteService: AutoCompleteService,
                                              val cc: MessagesControllerComponents,
                                              additional_address_UK: additional_address_UK,
                                              implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection with AddressHelper {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, Some(ResponsiblePersonAddressHistory(_, Some(additionalAddress), _)), _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(additional_address_UK(Form2[ResponsiblePersonAddress](additionalAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
          Ok(additional_address_UK(EmptyForm, edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        (Form2[ResponsiblePersonAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(additional_address_UK(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
              (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                additionalAddress <- addressHistory.additionalAddress
              } yield {
                val additionalAddressWithTime = data.copy(timeAtAddress = additionalAddress.timeAtAddress)
                updateAdditionalAddressAndRedirect(request.credId, additionalAddressWithTime, index, edit, flow)
              }) getOrElse updateAdditionalAddressAndRedirect(request.credId, data, index, edit, flow)
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }
}
