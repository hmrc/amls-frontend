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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople.{ResponsiblePerson, _}
import models.status.SubmissionStatus
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.StatusService
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.time_at_address

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TimeAtCurrentAddressController @Inject () (
                                                val dataCacheConnector: DataCacheConnector,
                                                authAction: AuthAction,
                                                val ds: CommonPlayDependencies,
                                                val statusService: StatusService,
                                                val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(_, Some(timeAtAddress), _)), _, _)), _, _, _, _, _, _, _, _, _, _, _, _)) =>
            Ok(time_at_address(Form2[TimeAtAddress](timeAtAddress), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)) =>
            Ok(time_at_address(Form2(DefaultAddressHistory), edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        (Form2[TimeAtAddress](request.body) match {
          case f: InvalidForm => getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(time_at_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data) => {
            getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
              (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                currentAddress <- addressHistory.currentAddress
              } yield {
                val currentAddressWithTime = currentAddress.copy(
                  timeAtAddress = Some(data)
                )
                doUpdate(request.credId, index, currentAddressWithTime).flatMap { _ =>
                  for {
                    status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
                  } yield {
                    redirectTo(index, data, rp, status, edit, flow)
                  }
                }
              }) getOrElse Future.successful(NotFound(notFoundView))
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
  }

  private def doUpdate(credId: String, index: Int, rp: ResponsiblePersonCurrentAddress)
                      (implicit request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
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
                         rp: ResponsiblePerson,
                         status: SubmissionStatus,
                         edit: Boolean,
                         flow: Option[String])(implicit request: Request[AnyContent]) = {
    timeAtAddress match {
      case ThreeYearsPlus | OneToThreeYears if !edit => Redirect(routes.PositionWithinBusinessController.get(index, edit, flow))
      case ThreeYearsPlus | OneToThreeYears if edit => Redirect(routes.DetailedAnswersController.get(index, flow))
      case _ => Redirect(routes.AdditionalAddressController.get(index, edit, flow))
    }
  }
}