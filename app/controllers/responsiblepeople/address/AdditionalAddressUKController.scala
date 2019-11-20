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

import cats.data.OptionT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import services.AutoCompleteService
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.additional_address_UK

import scala.concurrent.Future

@Singleton
class AdditionalAddressUKController @Inject()(override val dataCacheConnector: DataCacheConnector,
                                              authAction: AuthAction,
                                              implicit val auditConnector: AuditConnector,
                                              val autoCompleteService: AutoCompleteService) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_, Some(ResponsiblePersonAddressHistory(_, Some(additionalAddress), _)),_,_,_,_,_,_,_,_,_,_,_, _)) =>
          Ok(additional_address_UK(Form2[ResponsiblePersonAddress](additionalAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(additional_address_UK(EmptyForm, edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        (Form2[ResponsiblePersonAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(additional_address_UK(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            getData[ResponsiblePerson](request.credId, index) flatMap { responsiblePerson =>
              (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                additionalAddress <- addressHistory.additionalAddress
              } yield {
                val additionalAddressWithTime = data.copy(timeAtAddress = additionalAddress.timeAtAddress)
                updateAndRedirect(request.credId, additionalAddressWithTime, index, edit, flow)
              }) getOrElse updateAndRedirect(request.credId, data, index, edit, flow)
            }
          }}).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }

  private def updateAndRedirect(credId: String, data: ResponsiblePersonAddress, index: Int, edit: Boolean, flow: Option[String])
                               (implicit request: Request[AnyContent]) = {
    val doUpdate = () => updateDataStrict[ResponsiblePerson](credId, index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) if data.timeAtAddress.contains(ThreeYearsPlus) | data.timeAtAddress.contains(OneToThreeYears) =>
            a.additionalAddress(data).removeAdditionalExtraAddress
          case Some(a) => a.additionalAddress(data)
          case _ => ResponsiblePersonAddressHistory(additionalAddress = Some(data))
        })
    } map { _ =>
      data.timeAtAddress match {
        case Some(_) if edit => Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index, flow))
        case _ => Redirect(routes.TimeAtAdditionalAddressController.get(index, edit, flow))
      }
    }

    (for {
      rp <- OptionT(getData[ResponsiblePerson](credId, index))
      _ <- OptionT.liftF(AddressHelper.auditPreviousAddressChange(data.personAddress, rp, edit)) orElse OptionT.some[Future, AuditResult](Success)
      result <- OptionT.liftF(doUpdate())
    } yield result) getOrElse NotFound(notFoundView)
  }
}