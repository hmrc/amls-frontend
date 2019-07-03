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

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.DateOfChange
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import org.joda.time.{LocalDate, Months}
import services.AutoCompleteService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople


import scala.concurrent.Future

@Singleton
class NewHomeAddressController @Inject()(val authConnector: AuthConnector,
                                         val dataCacheConnector: DataCacheConnector,
                                         val autoCompleteService: AutoCompleteService) extends RepeatingSection with BaseController {

  final val DefaultAddressHistory = NewHomeAddress(PersonAddressUK("", "", None, None, ""))

  def get(index: Int) = Authorised.async {
      implicit authContext =>
        implicit request =>
          getData[ResponsiblePerson](index) map {
            case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
            => Ok(responsiblepeople.new_home_address(Form2(DefaultAddressHistory), index, personName.titleName, autoCompleteService.getCountries))
            case _
            => NotFound(notFoundView)
          }
    }

  def post(index: Int) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          (Form2[NewHomeAddress](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePerson](index) map { rp =>
                BadRequest(responsiblepeople.new_home_address(f, index, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
              }
            case ValidForm(_, data) => {
              for {
                moveDate <- dataCacheConnector.fetch[NewHomeDateOfChange](NewHomeDateOfChange.key)
                _ <- updateDataStrict[ResponsiblePerson](index) { rp =>
                  rp.addressHistory(convertToCurrentAddress(data, moveDate, rp))
                }
                _ <- dataCacheConnector.save[NewHomeDateOfChange](NewHomeDateOfChange.key, NewHomeDateOfChange(None))
              } yield {
                Redirect(routes.DetailedAnswersController.get(index))
              }
            }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
    }

  private def getTimeAtAddress(dateOfMove: Option[NewHomeDateOfChange]): Option[TimeAtAddress] = {
    dateOfMove flatMap {
      dateOp =>
        dateOp.dateOfChange map {date =>
          Months.monthsBetween(date, LocalDate.now()).getMonths match {
            case m if 0 until 6 contains m => ZeroToFiveMonths
            case m if 6 until 12 contains m => SixToElevenMonths
            case m if 12 until 36 contains m => OneToThreeYears
            case _ => ThreeYearsPlus
          }
        }
    }
  }

  private def pushCurrentAddress(currentAddress: Option[ResponsiblePersonCurrentAddress]): Option[ResponsiblePersonAddress] = {
    currentAddress.fold[Option[ResponsiblePersonAddress]](None)(x => Some(ResponsiblePersonAddress(x.personAddress, x.timeAtAddress)))
  }

  private def getUpdatedAddrAndExtraAddr(rp: ResponsiblePerson, currentTimeAtAddress: Option[TimeAtAddress]) = {
    currentTimeAtAddress match {
      case Some(ZeroToFiveMonths) | Some(SixToElevenMonths) => rp.addressHistory.fold[(Option[ResponsiblePersonAddress],
        Option[ResponsiblePersonAddress])]((None, None))(addrHistory => (pushCurrentAddress(addrHistory.currentAddress), addrHistory.additionalAddress))
      case _ => (None, None)
    }
  }

  private def convertToCurrentAddress(addr: NewHomeAddress, dateOfMove: Option[NewHomeDateOfChange], rp: ResponsiblePerson) = {
    val currentTimeAtAddress = getTimeAtAddress(dateOfMove)
    val (additionalAddress, extraAdditionalAddress) = getUpdatedAddrAndExtraAddr(rp, currentTimeAtAddress)

    ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(addr.personAddress,
      currentTimeAtAddress,
      dateOfMove.fold[Option[DateOfChange]](None)(x => x.dateOfChange.map(DateOfChange(_))))),
      None,
      additionalAddress,
      extraAdditionalAddress)
  }
}
