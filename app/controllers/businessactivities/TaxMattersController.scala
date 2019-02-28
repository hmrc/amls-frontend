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

package controllers.businessactivities

import cats.data.OptionT
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._
import cats.implicits._
import models.changeofficer.{ChangeOfficer, NewOfficer, RoleInBusiness}
import models.responsiblepeople.ResponsiblePerson
import utils.StatusConstants

import scala.concurrent.Future

class TaxMattersController @Inject() (val dataCacheConnector: DataCacheConnector,
                                      override val authConnector: AuthConnector
                                     ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
          response =>
            val form = (for {
              businessActivities <- response
              taxMatters <- businessActivities.taxMatters
            } yield Form2[TaxMatters](taxMatters)).getOrElse(EmptyForm)

           
            val asd = (for {
              name <- getAccountantName()
            } yield name).value.value.get.get
            Ok(tax_matters(form, edit, asd))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[TaxMatters](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(tax_matters(f, edit)))
          case ValidForm(_, data) =>
            for {
              businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
              _ <- dataCacheConnector.save[BusinessActivities](
                BusinessActivities.key,
                businessActivities.taxMatters(Some(data))
              )
            } yield Redirect(routes.SummaryController.get())
        }
  }

  def getAccountantName()(implicit headerCarrier: HeaderCarrier, authContext: AuthContext) = {
    for {
      activities: BusinessActivities <- OptionT(dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key))
      asdasd <- OptionT.fromOption[Future](activities.whoIsYourAccountant)
    } yield asdasd.accountantsName
  }
}