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

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.DateOfChange
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import org.joda.time.{LocalDate, Months}
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople
import views.html.responsiblepeople.address.new_home_address

import scala.concurrent.Future

@Singleton
class NewHomeAddressController @Inject()(authAction: AuthAction,
                                         val dataCacheConnector: DataCacheConnector,
                                         val autoCompleteService: AutoCompleteService) extends RepeatingSection with DefaultBaseController {

  def get(index: Int) = authAction.async {
        implicit request =>
          for {
            rp <- getData[ResponsiblePerson](request.credId, index)
            newAddress <- dataCacheConnector.fetch[NewHomeAddress](request.credId, NewHomeAddress.key)
          } yield (rp, newAddress) match {
            case (Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)), Some(newHomeAddress))
            => Ok(new_home_address(Form2[NewHomeAddress](newHomeAddress), index, personName.titleName, autoCompleteService.getCountries))
            case (Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)), None)
            => Ok(new_home_address(EmptyForm, index, personName.titleName, autoCompleteService.getCountries))
            case _
            => NotFound(notFoundView)
          }


//          getData[ResponsiblePerson](request.credId, index) map {
//            case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
//            => Ok(new_home_address(EmptyForm, index, personName.titleName, autoCompleteService.getCountries))
//            case _
//            => NotFound(notFoundView)
//          }
    }

  def post(index: Int) =
    authAction.async {
        implicit request =>

          def processForm(data: NewHomeAddress) = {
            for {
              redirect <- dataCacheConnector.save[NewHomeAddress](request.credId, NewHomeAddress.key, data) map { _ =>
                if (data.personAddress.isInstanceOf[PersonAddressUK]) {
                  Redirect(routes.NewHomeAddressUKController.get(index))
                } else {
                  Redirect(routes.NewHomeAddressNonUKController.get(index))
                }
              }
            } yield redirect
          }

          (Form2[NewHomeAddress](request.body)(NewHomeAddress.addressFormRule(PersonAddress.formRule(AddressType.NewHome))) match {
            case f: InvalidForm if f.data.get("isUK").isDefined
            => processForm(NewHomeAddress(AddressHelper.modelFromForm(f)))
            case f: InvalidForm
            => getData[ResponsiblePerson](request.credId, index) map { rp =>
                BadRequest(new_home_address(f, index, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
              }
            case ValidForm(_, data)
            => processForm(data)
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
    }
}
