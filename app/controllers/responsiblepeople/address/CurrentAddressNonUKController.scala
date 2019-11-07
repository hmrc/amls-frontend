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

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import models.status.SubmissionStatus
import play.api.mvc.{AnyContent, Request}
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, ControllerHelper, DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.current_address

import scala.concurrent.Future

class CurrentAddressNonUKController @Inject ()(
                                            val dataCacheConnector: DataCacheConnector,
                                            auditConnector: AuditConnector,
                                            autoCompleteService: AutoCompleteService,
                                            statusService: StatusService,
                                            authAction: AuthAction
                                          ) extends RepeatingSection with DefaultBaseController with DateOfChangeHelper {

  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,
          Some(ResponsiblePersonAddressHistory(Some(currentAddress),_,_)),_,_,_,_,_,_,_,_,_,_,_, _))
          => Ok(current_address(Form2[ResponsiblePersonCurrentAddress](currentAddress), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(current_address(Form2(DefaultAddressHistory), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
        implicit request =>
          (Form2[ResponsiblePersonCurrentAddress](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePerson](request.credId, index) map { rp =>
                BadRequest(current_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
              }
            case ValidForm(_, data) => {
              getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
                val currentAddressWithTime = (for {
                  rp <- responsiblePerson
                  addressHistory <- rp.addressHistory
                  currentAddress <- addressHistory.currentAddress
                } yield data.copy(timeAtAddress = currentAddress.timeAtAddress)).getOrElse(data)

                statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
                  status => updateAndRedirect(request.credId, currentAddressWithTime, index, edit, flow, responsiblePerson, status)
                }
              }
            }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
    }

  private def updateAndRedirect
  (credId: String, data: ResponsiblePersonCurrentAddress, index: Int, edit: Boolean, flow: Option[String], originalResponsiblePerson: Option[ResponsiblePerson],
   status: SubmissionStatus)
  (implicit request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) => a.currentAddress(data)
          case _ => ResponsiblePersonAddressHistory(currentAddress = Some(data))
        })
    } flatMap { _ =>
      if (edit) {
        val originalAddress = for {
          rp <- originalResponsiblePerson
          rpHistory <- rp.addressHistory
          rpCurrAddr <- rpHistory.currentAddress
        } yield rpCurrAddr.personAddress
        auditConnector.sendEvent(AddressModifiedEvent(data.personAddress, originalAddress)) map { _ =>
          if (redirectToDateOfChange[PersonAddress](status, originalAddress, data.personAddress)
            && originalResponsiblePerson.flatMap {
            orp => orp.lineId
          }.isDefined && originalAddress.isDefined) {
            Redirect(routes.CurrentAddressDateOfChangeController.get(index, edit))
          } else {
            Redirect(routes.DetailedAnswersController.get(index, flow))
          }
        }
      } else {
        auditConnector.sendEvent(AddressCreatedEvent(data.personAddress)) map { _ =>
          Redirect(routes.TimeAtCurrentAddressController.get(index, edit, flow))
        }
      }
    }
  }
}