/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople.{ResponsiblePeople, _}
import models.status.{SubmissionDecisionApproved, SubmissionStatus}
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.time_at_address

import scala.concurrent.Future

trait TimeAtCurrentAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  val statusService: StatusService

  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[ResponsiblePeople](index) map {
        case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(_,Some(timeAtAddress),_)),_,_)),_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(time_at_address(Form2[TimeAtAddress](timeAtAddress), edit, index, fromDeclaration, personName.titleName))
        case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(time_at_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      (Form2[TimeAtAddress](request.body) match {
        case f: InvalidForm => getData[ResponsiblePeople](index) map { rp =>
            BadRequest(time_at_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
          }
        case ValidForm(_, data) => {
          getData[ResponsiblePeople](index) flatMap { responsiblePerson =>
            (for {
              rp <- responsiblePerson
              addressHistory <- rp.addressHistory
              currentAddress <- addressHistory.currentAddress
            } yield {
              val currentAddressWithTime = currentAddress.copy(
                timeAtAddress = Some(data)
              )
              doUpdate(index, currentAddressWithTime).flatMap { _  =>
                for {
                  status <- statusService.getStatus
                } yield {
                  redirectTo(index,data,rp,status, edit, fromDeclaration)
                }
              }
            }) getOrElse Future.successful(NotFound(notFoundView))
          }
        }
      }).recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
  }

  private def doUpdate(index: Int, rp: ResponsiblePersonCurrentAddress)
                      (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(_) if rp.timeAtAddress.contains(OneToThreeYears) | rp.timeAtAddress.contains(ThreeYearsPlus) =>
            ResponsiblePersonAddressHistory(currentAddress = Some(rp))
          case Some(a) => a.currentAddress(rp)
          case _ => ResponsiblePersonAddressHistory(currentAddress = Some(rp))
        })
    }
  }

  private def redirectTo(index: Int, timeAtAddress: TimeAtAddress,
                        rp: ResponsiblePeople,
                        status: SubmissionStatus,
                        edit: Boolean,
                        fromDeclaration: Boolean)(implicit request:Request[AnyContent]) = {
    timeAtAddress match {
      case ThreeYearsPlus | OneToThreeYears if !edit => Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
      case ThreeYearsPlus | OneToThreeYears if edit => Redirect(routes.DetailedAnswersController.get(index, edit))
      case _ => Redirect(routes.AdditionalAddressController.get(index, edit, fromDeclaration))
    }
  }

}

object TimeAtCurrentAddressController extends TimeAtCurrentAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  val statusService = StatusService

  override def dataCacheConnector = DataCacheConnector
}