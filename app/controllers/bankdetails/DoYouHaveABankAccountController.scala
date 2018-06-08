/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BooleanFormReadWrite
import views.html.bankdetails._

import scala.concurrent.Future

class DoYouHaveABankAccountController @Inject()(val authConnector: AuthConnector) extends BaseController {

  val router: Boolean => Call = {
    case true => routes.BankAccountNameController.get(1)
    case _ => routes.YourBankAccountsController.get()
  }

  def view(implicit request: Request[_]) = has_bank_account.apply _

  implicit val formReads = BooleanFormReadWrite.formRule("hasBankAccount", "bankdetails.hasbankaccount.validation")

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(view(request)(EmptyForm)))
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[Boolean](request.body) match {
          case ValidForm(_, data) => Future.successful(Redirect(router(data)))

          case f: InvalidForm => Future.successful(BadRequest(view(request)(f)))
        }
  }
}
