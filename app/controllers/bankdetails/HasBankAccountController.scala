/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.bankdetails.BankDetails
import play.api.mvc.{Call, MessagesControllerComponents, Request}
import utils.{AuthAction, BooleanFormReadWrite}
import views.html.bankdetails._

import scala.concurrent.Future

class HasBankAccountController @Inject()(val authAction: AuthAction,
                                         val ds: CommonPlayDependencies,
                                         cacheConnector: DataCacheConnector,
                                         val cc: MessagesControllerComponents,
                                         has_bank_account: has_bank_account) extends AmlsBaseController(ds, cc) {

  val router: Boolean => Call = {
    case true => routes.BankAccountNameController.getNoIndex
    case _ => routes.YourBankAccountsController.get
  }

  def view(implicit request: Request[_]) = has_bank_account.apply _

  implicit val formReads = BooleanFormReadWrite.formRule("hasBankAccount", "bankdetails.hasbankaccount.validation")

  def get = authAction.async {
      implicit request =>
        Future.successful(Ok(view.apply(EmptyForm)))
  }

  def post = authAction.async {
      implicit request =>
        Form2[Boolean](request.body) match {
          case ValidForm(_, data) if data =>
            Future.successful(Redirect(router(data)))

          case ValidForm(_, data) =>
            cacheConnector.save(request.credId, BankDetails.key, Seq.empty[BankDetails]) map { _ => Redirect(router(data)) }

          case f: InvalidForm => Future.successful(BadRequest(view.apply(f)))
        }
  }
}
