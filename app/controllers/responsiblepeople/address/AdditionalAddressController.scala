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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import services.AutoCompleteService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.additional_address

import scala.concurrent.Future

@Singleton
class AdditionalAddressController @Inject()(
                                              override val dataCacheConnector: DataCacheConnector,
                                              authAction: AuthAction,
                                              auditConnector: AuditConnector,
                                              val autoCompleteService: AutoCompleteService
                                            ) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_, Some(ResponsiblePersonAddressHistory(_, Some(additionalAddress), _)),_,_,_,_,_,_,_,_,_,_,_, _)) =>
            Ok(additional_address(Form2[ResponsiblePersonAddress](additionalAddress), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(additional_address(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        def processForm(data: ResponsiblePersonAddress) = {
          data.personAddress match {
            case _: PersonAddressUK => Future.successful(Redirect(routes.AdditionalAddressUKController.get(index, edit, flow)))
            case _: PersonAddressNonUK => Future.successful(Redirect(routes.AdditionalAddressNonUKController.get(index, edit, flow)))
          }
        }

        (Form2[ResponsiblePersonAddress](request.body) match {
          case f: InvalidForm if f.data.get("isUK").isDefined => processForm(ResponsiblePersonAddress(processAsValid(f), None))
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(additional_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            processForm(data)
          }}).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }

  //todo: populate the models correctly
  def processAsValid(f: InvalidForm): PersonAddress = {
    if(f.data.get("isUK").contains(Seq("true"))){
      PersonAddressUK("", "", None, None, "")
    } else {
      PersonAddressNonUK("", "", None, None, Country("", ""))
    }
  }
}
