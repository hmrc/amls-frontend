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

package controllers.responsiblepeople.address

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import models.status.SubmissionStatus
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, ControllerHelper, DateOfChangeHelper}
import views.html.responsiblepeople.address.current_address_UK

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CurrentAddressUKController @Inject ()(val dataCacheConnector: DataCacheConnector,
                                            auditConnector: AuditConnector,
                                            autoCompleteService: AutoCompleteService,
                                            statusService: StatusService,
                                            authAction: AuthAction,
                                            val ds: CommonPlayDependencies,
                                            val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection with DateOfChangeHelper {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _,
        Some(ResponsiblePersonAddressHistory(Some(currentAddress), _, _)), _, _, _, _, _, _, _, _, _, _, _, _))
        => Ok(current_address_UK(Form2[ResponsiblePersonCurrentAddress](currentAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
        => Ok(current_address_UK(EmptyForm, edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        (Form2[ResponsiblePersonCurrentAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(current_address_UK(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
              val currentAddressWithTime = (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                currentAddress <- addressHistory.currentAddress
              } yield data.copy(timeAtAddress = currentAddress.timeAtAddress)).getOrElse(data)

              statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
                status => updateCurrentAddressAndRedirect(request.credId, currentAddressWithTime, index, edit, flow, responsiblePerson, status)
              }
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }
}