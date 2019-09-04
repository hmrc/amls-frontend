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

package controllers.changeofficer

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.changeofficer.Helpers._
import forms.{Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.BusinessMatching
import models.changeofficer.{ChangeOfficer, Role, RoleInBusiness}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction

import scala.concurrent.Future

class RoleInBusinessController @Inject()
(authAction: AuthAction, val ds: CommonPlayDependencies, implicit val dataCacheConnector: DataCacheConnector) extends AmlsBaseController(ds) {
  def get = authAction.async {
    implicit request =>

      val result = for {
        co <- OptionT(dataCacheConnector.fetch[ChangeOfficer](request.credId, ChangeOfficer.key)) orElse OptionT.some(ChangeOfficer(RoleInBusiness(Set.empty[Role])))
        (businessType, name) <- getBusinessNameAndName(request.credId)
      } yield {
        Ok(views.html.changeofficer.role_in_business(Form2[RoleInBusiness](co.roleInBusiness), businessType, name))
      }
      result getOrElse InternalServerError("Unable to get nominated officer")
  }

  def post() = authAction.async {
    implicit request =>
      Form2[RoleInBusiness](request.body) match {
        case f: InvalidForm =>
          val result = getBusinessNameAndName(request.credId) map {
            case (businessType, name) => BadRequest(views.html.changeofficer.role_in_business(f, businessType, name))
          }

          result getOrElse InternalServerError("Unable to get nominated officer")

        case ValidForm(_, data) =>
          dataCacheConnector.save(request.credId, ChangeOfficer.key, ChangeOfficer(data)) map { _ =>
            if(data.roles.isEmpty){
              Redirect(routes.RemoveResponsiblePersonController.get())
            } else {
              Redirect(routes.NewOfficerController.get())
            }
          }
      }
  }

  private def getBusinessNameAndName(credId: String)(implicit headerCarrier: HeaderCarrier) = {
    for {
      businessMatching <- OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
      reviewDetails <- OptionT.fromOption[Future](businessMatching.reviewDetails)
      businessType <- OptionT.fromOption[Future](reviewDetails.businessType)
      name <- getNominatedOfficerName(credId)
    } yield (businessType, name)
  }
}