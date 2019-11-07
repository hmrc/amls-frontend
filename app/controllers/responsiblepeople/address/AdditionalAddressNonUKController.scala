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
import cats.data.OptionT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import controllers.responsiblepeople.routes
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import services.AutoCompleteService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.additional_address

import scala.concurrent.Future

@Singleton
class AdditionalAddressNonUKController @Inject()(
                                              override val dataCacheConnector: DataCacheConnector,
                                              authAction: AuthAction,
                                              auditConnector: AuditConnector,
                                              val autoCompleteService: AutoCompleteService
                                            ) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_, Some(ResponsiblePersonAddressHistory(_, Some(additionalAddress), _)),_,_,_,_,_,_,_,_,_,_,_, _)) =>
          Ok(additional_address(Form2[ResponsiblePersonAddress](additionalAddress), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(additional_address(Form2(ResponsiblePersonAddressHistory.default()), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        (Form2[ResponsiblePersonCurrentAddress](request.body) match {
          case f: InvalidForm =>
            Future.successful(Redirect(routes.TimeAtAdditionalAddressController.get(index, edit, flow)))
          case ValidForm(_, data) => {
            Future.successful(Redirect(routes.TimeAtAdditionalAddressController.get(index, edit, flow)))
          }}).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }
}
