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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import javax.inject.{Inject, Singleton}
import jto.validation.Write
import jto.validation.forms.UrlFormEncoded
import models.DateOfChange
import models.flowmanagement.RemoveBusinessTypeFlowModel
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.date_of_change

import scala.concurrent.Future

@Singleton
class WhatDateRemovedController @Inject()(
                                                   val authConnector: AuthConnector,
                                                   val dataCacheConnector: DataCacheConnector
                                                   ) extends BaseController with RepeatingSection {

  implicit val dateWrites: Write[DateOfChange, UrlFormEncoded] =
    Write {
      case DateOfChange(b) => Map(
        "dateOfChange.day" -> Seq(b.getDayOfMonth.toString),
        "dateOfChange.month" -> Seq(b.getMonthOfYear.toString),
        "dateOfChange.year" -> Seq(b.getYear.toString)
      )
    }

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      getFormData map { case (model) =>
        val form = model.dateOfChange map { v => Form2(v) } getOrElse EmptyForm
        println(form)
        Ok(date_of_change(form, "summary.updateservice", routes.WhatDateRemovedController.post()))
      } getOrElse  InternalServerError("Get: Unable to show date_of_change Activities page. Failed to retrieve data")
    }

  def post = Authorised.async {
    implicit authContext =>
      implicit request => ???
  }

  private def getFormData(implicit hc: HeaderCarrier, ac: AuthContext): OptionT[Future, (RemoveBusinessTypeFlowModel)] = for {
    model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](RemoveBusinessTypeFlowModel.key))
  } yield (model)

}