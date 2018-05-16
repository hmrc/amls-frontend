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

package views.deregister

import generators.AmlsReferenceNumberGenerator
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class deregister_applicationSpec extends AmlsSpec with MustMatchers with AmlsReferenceNumberGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val businessName = "Test Business"
    val currentRegYearEndDate = LocalDate.now()

    def view = views.html.deregister.deregister_application(businessName, Set.empty, amlsRegistrationNumber)
  }

  "deregister_application view" must {
    "have correct title and headings" in new ViewFixture {

      val title = s"${Messages("status.deregister.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

      doc.title mustBe title
      heading.html must be(Messages("status.deregister.title"))
      subHeading.html must include(Messages("summary.status"))

    }

    "have correct body content" in new ViewFixture {
      validateParagraphizedContent("status.deregister.body-content")

      doc.getElementById("change-services").attr("href") mustBe controllers.businessmatching.updateservice.routes.ChangeBusinessTypesController.get().url
    }
  }
}
