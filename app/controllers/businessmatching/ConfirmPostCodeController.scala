/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.Country
import models.businesscustomer.ReviewDetails
import models.businessmatching.{BusinessMatching, ConfirmPostcode}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction

import views.html.businessmatching.confirm_postcode

import scala.concurrent.Future

@Singleton
class ConfirmPostCodeController @Inject()(authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val dataCacheConnector: DataCacheConnector,
                                          val cc: MessagesControllerComponents,
                                          confirm_postcode: confirm_postcode) extends AmlsBaseController(ds, cc) {


  def get() = authAction.async {
      implicit request =>
        Future.successful(Ok(confirm_postcode(EmptyForm)))
  }

  def updateReviewDetails(reviewDetails: Option[ReviewDetails], postCodeModel: ConfirmPostcode): Option[models.businesscustomer.ReviewDetails] = {
    reviewDetails.fold[Option[ReviewDetails]](None) { dtls =>
      val updatedAddr = dtls.businessAddress.copy(postcode = Some(postCodeModel.postCode), country = Country("United Kingdom", "GB"))
      Some(dtls.copy(businessAddress = updatedAddr))
    }
  }

  def post() = authAction.async {
      implicit request => {
        Form2[ConfirmPostcode](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(confirm_postcode(f)))
          case ValidForm(_, data) =>
            for {
              bm <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
              _ <- dataCacheConnector.save[BusinessMatching](request.credId, BusinessMatching.key,
                bm.copy(reviewDetails = updateReviewDetails(bm.reviewDetails, data)))
            } yield {
              Redirect(routes.BusinessTypeController.get)
            }
        }
      }
  }
}


