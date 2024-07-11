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

package views.notifications

import config.ApplicationConfig
import models.notifications.NotificationParams
import play.api.i18n.{Lang, Messages}
import play.api.mvc.Request
import play.twirl.api.{BaseScalaTemplate, Format, HtmlFormat, Template5}

trait VersionedView {
  type NotificationViewScalaTemplate5 = BaseScalaTemplate[HtmlFormat.Appendable, Format[HtmlFormat.Appendable]]
    with Template5[NotificationParams, Request[_], Messages, Lang, ApplicationConfig, HtmlFormat.Appendable]

  def viewFromTemplateFilename(templateName: String): NotificationViewScalaTemplate5
}
