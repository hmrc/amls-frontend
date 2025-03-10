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

import com.google.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.address.AdditionalAddressUKFormProvider
import models.responsiblepeople._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.AdditionalAddressUKView

import scala.concurrent.Future

@Singleton
class AdditionalAddressUKController @Inject() (
  override val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val auditConnector: AuditConnector,
  val cc: MessagesControllerComponents,
  formProvider: AdditionalAddressUKFormProvider,
  view: AdditionalAddressUKView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with AddressHelper {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.addressHistory) match {
            case (Some(name), Some(ResponsiblePersonAddressHistory(_, Some(additionalAddress), _))) =>
              Ok(view(formProvider().fill(additionalAddress), edit, index, flow, name.titleName))
            case (Some(name), _)                                                                    =>
              Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _                                                                                  => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] =
    authAction.async { implicit request =>
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
                rp                <- responsiblePerson
                addressHistory    <- rp.addressHistory
                additionalAddress <- addressHistory.additionalAddress
              } yield {
                val additionalAddressWithTime = data.copy(timeAtAddress = additionalAddress.timeAtAddress)
                updateAdditionalAddressAndRedirect(request.credId, additionalAddressWithTime, index, edit, flow)
              }) getOrElse updateAdditionalAddressAndRedirect(request.credId, data, index, edit, flow)
            }
        )
        .recoverWith { case _: IndexOutOfBoundsException =>
          Future.successful(NotFound(notFoundView))
        }
    }
}
