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

package models.businessmatching.updateservice

import models.businessmatching.{BusinessActivity, BusinessMatchingMsbService}
import play.api.libs.json.Json

case class ServiceChangeRegister(addedActivities: Option[Set[BusinessActivity]] = None,
                                 addedSubSectors: Option[Set[BusinessMatchingMsbService]] = None)

object ServiceChangeRegister {
  val key = "service-change-register"

  implicit val format = Json.format[ServiceChangeRegister]
}
