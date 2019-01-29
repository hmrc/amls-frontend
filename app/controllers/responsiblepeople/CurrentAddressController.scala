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

package controllers.responsiblepeople

import audit.{AddressCreatedEvent, AddressModifiedEvent}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import models.status.SubmissionStatus
import play.api.mvc.{AnyContent, Request}
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.current_address
import audit.AddressConversions._
import com.google.inject.Inject
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class CurrentAddressController @Inject () (
                                            override val dataCacheConnector: DataCacheConnector,
                                            auditConnector: AuditConnector,
                                            autoCompleteService: AutoCompleteService,
                                            statusService: StatusService,
                                            override val authConnector: AuthConnector
                                          ) extends RepeatingSection with BaseController with DateOfChangeHelper {

  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
      implicit authContext => implicit request =>

        getData[ResponsiblePerson](index) map {
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
    Authorised.async {
      implicit authContext =>
        implicit request =>
          (Form2[ResponsiblePersonCurrentAddress](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePerson](index) map { rp =>
                BadRequest(current_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
              }
            case ValidForm(_, data) => {
              getData[ResponsiblePerson](index) flatMap { responsiblePerson =>
                val currentAddressWithTime = (for {
                  rp <- responsiblePerson
                  addressHistory <- rp.addressHistory
                  currentAddress <- addressHistory.currentAddress
                } yield data.copy(timeAtAddress = currentAddress.timeAtAddress)).getOrElse(data)

                statusService.getStatus flatMap {
                  status => updateAndRedirect(currentAddressWithTime, index, edit, flow, responsiblePerson, status)
                }
              }
            }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
    }

  private def updateAndRedirect
  (data: ResponsiblePersonCurrentAddress, index: Int, edit: Boolean, flow: Option[String], originalResponsiblePerson: Option[ResponsiblePerson],
   status: SubmissionStatus)
  (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePerson](index) { res =>
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