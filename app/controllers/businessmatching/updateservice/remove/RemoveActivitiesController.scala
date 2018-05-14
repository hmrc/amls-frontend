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

import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Path, Rule, RuleLike}
import models.FormTypes
import models.businessmatching.{BusinessActivities, BusinessActivity}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class RemoveActivitiesController @Inject()(
                                          val authConnector: AuthConnector,
                                          val dataCacheConnector: DataCacheConnector
                                          ) extends BaseController {

  implicit def formReads(implicit p: Path => RuleLike[UrlFormEncoded, Set[BusinessActivity]]): Rule[UrlFormEncoded, BusinessActivities] =
    FormTypes.businessActivityRule("error.required.bm.remove.service")

  def get = Authorised.async{
    implicit authContext =>
      implicit request => ???
  }

  def post = Authorised.async{
    implicit authContext =>
      implicit request => ???
  }

}
