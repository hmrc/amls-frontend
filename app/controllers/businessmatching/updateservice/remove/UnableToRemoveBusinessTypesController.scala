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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessMatching
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.businessmatching.updateservice.remove.UnableToRemoveActivityView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UnableToRemoveBusinessTypesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  view: UnableToRemoveActivityView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    getBusinessActivity(request.credId) map { activity =>
      Ok(view(activity))
    } getOrElse (InternalServerError("Get: Unable to show Unable to Remove Activities page"))
  }

  private def getBusinessActivity(credId: String)(implicit messages: Messages): OptionT[Future, String] = for {
    model      <- OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
    activities <- OptionT.fromOption[Future](model.alphabeticalBusinessActivitiesLowerCase(false))
  } yield activities.head

}
