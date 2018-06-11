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

import javax.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Write}
import models.FormTypes
import models.bankdetails.BankDetails
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.Future

@Singleton
class BankAccountNameController @Inject()(
                                           val authConnector: AuthConnector,
                                           val dataCacheConnector: DataCacheConnector,
                                           implicit val statusService: StatusService
                                         ) extends RepeatingSection with BaseController {

  implicit def write: Write[String, UrlFormEncoded] = Write { data =>
    Map("accountName" -> Seq(data))
  }

  implicit val read = From[UrlFormEncoded] { __ =>
    (__ \ "accountName").read(FormTypes.accountNameType)
  }

  def get(index: Option[Int] = None, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        index match {
          case Some(i) =>
            getData[BankDetails](i) map {
              case Some(BankDetails(_, Some(data), _, _, _, _, _)) =>
                Ok(views.html.bankdetails.bank_account_name(Form2[String](data), edit, Some(i)))
              case Some(_) =>
                Ok(views.html.bankdetails.bank_account_name(EmptyForm, edit, Some(i)))
            }
          case _ => Future.successful(NotFound(notFoundView))
        }
  }

  def post(index: Option[Int] = None, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[String](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.bankdetails.bank_account_name(f, edit, index)))
          case ValidForm(_, data) =>
            val newBankDetails = BankDetails(accountName = Some(data))
            index match {
              case Some(i) => updateDataStrict[BankDetails](i) { bd =>
                bd.copy(
                  accountName = Some(data),
                  status = Some(if (edit) {
                    StatusConstants.Updated
                  } else {
                    StatusConstants.Added
                  })
                )
              } map { _ =>
                if (edit) {
                  Redirect(routes.SummaryController.get(i))
                } else {
                  Redirect(routes.BankAccountTypeController.get(i))
                }
              }
              case _ => dataCacheConnector.fetch[Seq[BankDetails]](BankDetails.key) flatMap { maybeBankDetails =>
                val newList = maybeBankDetails.getOrElse(Seq.empty) ++ Seq(newBankDetails)
                dataCacheConnector.save(BankDetails.key, newList) map { _ => Redirect(routes.BankAccountTypeController.get(index.get)) }
              }
            }
        }
      } recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
  }
}