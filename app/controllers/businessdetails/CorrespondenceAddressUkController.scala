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

package controllers.businessdetails

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import cats.data.OptionT
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessdetails.CorrespondenceAddressUKFormProvider
import models.businessdetails.{BusinessDetails, CorrespondenceAddress, CorrespondenceAddressUk}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.AutoCompleteService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.AuthAction
import views.html.businessdetails.CorrespondenceAddressUKView

import scala.concurrent.Future

class CorrespondenceAddressUkController @Inject() (
  val dataConnector: DataCacheConnector,
  val auditConnector: AuditConnector,
  val autoCompleteService: AutoCompleteService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: CorrespondenceAddressUKFormProvider,
  view: CorrespondenceAddressUKView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map { response =>
      val form = for {
        businessDetails       <- response
        correspondenceAddress <- businessDetails.correspondenceAddress
      } yield correspondenceAddress.ukAddress.fold(formProvider())(formProvider().fill)
      Ok(view(form.getOrElse(formProvider()), edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data => {
          val doUpdate = for {
            businessDetails: BusinessDetails <-
              OptionT(dataConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key))
            _                                <- OptionT.liftF(
                                                  dataConnector.save[BusinessDetails](
                                                    request.credId,
                                                    BusinessDetails.key,
                                                    businessDetails.correspondenceAddress(CorrespondenceAddress(Some(data), None))
                                                  )
                                                )
            _                                <- OptionT.liftF(
                                                  auditAddressChange(data, businessDetails.correspondenceAddress.flatMap(a => a.ukAddress), edit)
                                                ) orElse OptionT.some(Success)
          } yield Redirect(routes.SummaryController.get)
          doUpdate getOrElse InternalServerError("Could not update correspondence address")
        }
      )
  }

  def auditAddressChange(
    currentAddress: CorrespondenceAddressUk,
    oldAddress: Option[CorrespondenceAddressUk],
    edit: Boolean
  )(implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] =
    if (edit) {
      auditConnector.sendEvent(AddressModifiedEvent(currentAddress, oldAddress))
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(currentAddress))
    }
}
