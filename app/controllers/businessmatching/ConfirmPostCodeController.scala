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

package controllers.businessmatching

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.ConfirmPostcodeFormProvider
import models.Country
import models.businesscustomer.ReviewDetails
import models.businessmatching.{BusinessMatching, ConfirmPostcode}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.businessmatching.ConfirmPostcodeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ConfirmPostCodeController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: ConfirmPostcodeFormProvider,
  view: ConfirmPostcodeView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction { implicit request =>
    Ok(view(formProvider()))
  }

  private def updateReviewDetails(
    reviewDetails: Option[ReviewDetails],
    postCodeModel: ConfirmPostcode
  ): Option[ReviewDetails] =
    reviewDetails.fold[Option[ReviewDetails]](None) { dtls =>
      val updatedAddr =
        dtls.businessAddress.copy(postcode = Some(postCodeModel.postCode), country = Country("United Kingdom", "GB"))
      Some(dtls.copy(businessAddress = updatedAddr))
    }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        data =>
          for {
            bm <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
            _  <- dataCacheConnector.save[BusinessMatching](
                    request.credId,
                    BusinessMatching.key,
                    bm.copy(reviewDetails = updateReviewDetails(bm.reviewDetails, data))
                  )
          } yield Redirect(routes.BusinessTypeController.get())
      )
  }
}
