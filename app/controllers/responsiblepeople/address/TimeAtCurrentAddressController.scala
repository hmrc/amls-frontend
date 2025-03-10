/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.responsiblepeople.address.TimeAtAddressFormProvider
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.StatusService
import services.cache.Cache
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.TimeAtAddressView

import scala.concurrent.Future

class TimeAtCurrentAddressController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val statusService: StatusService,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: TimeAtAddressFormProvider,
  view: TimeAtAddressView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", None, None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.addressHistory) match {
            case (
                  Some(name),
                  Some(
                    ResponsiblePersonAddressHistory(
                      Some(ResponsiblePersonCurrentAddress(_, Some(timeAtAddress), _)),
                      _,
                      _
                    )
                  )
                ) =>
              Ok(view(formProvider().fill(timeAtAddress), edit, index, flow, name.titleName))
            case (Some(name), _) =>
              Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _               => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            },
          data =>
            getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
              (for {
                rp             <- responsiblePerson
                addressHistory <- rp.addressHistory
                currentAddress <- addressHistory.currentAddress
              } yield {
                val currentAddressWithTime = currentAddress.copy(
                  timeAtAddress = Some(data)
                )
                doUpdate(request.credId, index, currentAddressWithTime).flatMap { _ =>
                  for {
                    status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
                  } yield redirectTo(index, data, edit, flow)
                }
              }) getOrElse Future.successful(NotFound(notFoundView))
            }
        )
        .recoverWith { case _: IndexOutOfBoundsException =>
          Future.successful(NotFound(notFoundView))
        }
  }

  private def doUpdate(credId: String, index: Int, rp: ResponsiblePersonCurrentAddress): Future[Cache] =
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      res.addressHistory(res.addressHistory match {
        case Some(_) if rp.timeAtAddress.contains(OneToThreeYears) | rp.timeAtAddress.contains(ThreeYearsPlus) =>
          ResponsiblePersonAddressHistory(currentAddress = Some(rp))
        case Some(a)                                                                                           => a.currentAddress(rp)
        case _                                                                                                 => ResponsiblePersonAddressHistory(currentAddress = Some(rp))
      })
    }

  private def redirectTo(index: Int, timeAtAddress: TimeAtAddress, edit: Boolean, flow: Option[String]): Result =
    timeAtAddress match {
      case ThreeYearsPlus | OneToThreeYears if !edit =>
        Redirect(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(index, edit, flow))
      case ThreeYearsPlus | OneToThreeYears if edit  =>
        Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index, flow))
      case _                                         => Redirect(routes.AdditionalAddressController.get(index, edit, flow))
    }
}
