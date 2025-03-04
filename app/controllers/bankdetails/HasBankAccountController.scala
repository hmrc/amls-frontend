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

package controllers.bankdetails

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.bankdetails.HasBankAccountFormProvider
import models.bankdetails.BankDetails
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.bankdetails.HasBankAccountView

import javax.inject.Inject
import scala.concurrent.Future

class HasBankAccountController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  cacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: HasBankAccountFormProvider,
  view: HasBankAccountView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    Future.successful(Ok(view.apply(formProvider())))
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view.apply(formWithErrors))),
        data =>
          if (data) {
            Future.successful(Redirect(routes.BankAccountNameController.getNoIndex))
          } else {
            cacheConnector.save(request.credId, BankDetails.key, Seq.empty[BankDetails]) map { _ =>
              Redirect(routes.YourBankAccountsController.get())
            }
          }
      )
  }
}
